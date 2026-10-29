const expiredMessage = document.getElementById('expired');
const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
const csrfHeader = document.querySelector('meta[name="csrf-header"]').content;
let lastSignal = 0;
function showExpiry() { expiredMessage.hidden = false; }
async function recordInteraction(event) {
  // This runs only in response to real keyboard, input or click events, never on mouse movement.
  if (!event.isTrusted || Date.now() - lastSignal < 5000) return;
  lastSignal = Date.now();
  try {
    const response = await fetch('/admission/activity', {method:'POST', headers:{[csrfHeader]:csrfToken}});
    if (response.status === 409) showExpiry();
  } catch (_) { /* A failed signal does not claim to extend the server's timer. */ }
}
['input','keydown','click'].forEach(name => document.addEventListener(name, recordInteraction, {passive:true}));
async function observeAdmission() {
  try {
    const response = await fetch('/admission/status', {cache:'no-store'});
    if (response.redirected || response.status === 403) { showExpiry(); return; }
    if (response.ok && (await response.json()).state !== 'ACTIVE') showExpiry();
  } catch (_) { /* Keep unsaved form text on screen during connection loss. */ }
  window.setTimeout(observeAdmission, 2000);
}
window.setTimeout(observeAdmission, 2000);
