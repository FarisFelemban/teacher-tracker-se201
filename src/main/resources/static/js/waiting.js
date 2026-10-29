const statusText = document.getElementById('queue-status');
async function checkQueue() {
  try {
    const response = await fetch('/admission/status?join=true', {cache:'no-store'});
    if (response.redirected || response.status === 403) { window.location.assign('/login'); return; }
    if (!response.ok) throw new Error('check failed');
    const result = await response.json();
    if (result.state === 'ACTIVE') { window.location.assign('/teacher/dashboard'); return; }
    statusText.textContent = result.state === 'BUSY'
      ? 'The queue is full. We will retry shortly.' : 'Your queue position: ' + result.position;
  } catch (_) { statusText.textContent = 'Connection interrupted. Retrying your place shortly.'; }
  window.setTimeout(checkQueue, 2000);
}
checkQueue();
