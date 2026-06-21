// =========================================================================
//  Excel Utils — Export Dashboard — JavaScript
//  Handles all 16 endpoints: CSV (4 methods × 2 header modes) +
//                             Excel (4 methods × 2 header modes)
// =========================================================================

// ── Endpoints ────────────────────────────────────────────────────────────────
const ENDPOINTS = {
  // CSV — auto headers
  csvStream        : '/api/users/download/csv',
  csvBytes         : '/api/users/download/csv/bytes',
  csvBase64        : '/api/users/download/csv/base64',
  csvFile          : '/api/users/download/csv/file',
  // CSV — custom headers
  csvCustom        : '/api/users/download/csv/custom-headers',
  csvCustomBytes   : '/api/users/download/csv/custom-headers/bytes',
  csvCustomBase64  : '/api/users/download/csv/custom-headers/base64',
  csvCustomFile    : '/api/users/download/csv/custom-headers/file',
  // Excel — auto headers
  excel            : '/api/users/download/xlsx',
  excelBytes       : '/api/users/download/xlsx/bytes',
  excelBase64      : '/api/users/download/xlsx/base64',
  excelFile        : '/api/users/download/xlsx/file',
  // Excel — custom headers
  excelCustom      : '/api/users/download/xlsx/custom-headers',
  excelCustomBytes : '/api/users/download/xlsx/custom-headers/bytes',
  excelCustomBase64: '/api/users/download/xlsx/custom-headers/base64',
  excelCustomFile  : '/api/users/download/xlsx/custom-headers/file',
};

// ── Tab Switching ────────────────────────────────────────────────────────────
function switchTab(tabName) {
  // Deactivate all tabs
  document.querySelectorAll('.tab').forEach(t => {
    t.classList.remove('active');
    t.setAttribute('aria-selected', 'false');
  });
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));

  // Activate selected
  const tab = document.getElementById('tab-' + tabName);
  const panel = document.getElementById('panel-' + tabName);
  if (tab)   { tab.classList.add('active'); tab.setAttribute('aria-selected', 'true'); }
  if (panel) panel.classList.add('active');
}

// ── Time Helper ──────────────────────────────────────────────────────────────
function timestamp() {
  const now = new Date();
  return now.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

// Set initial time
document.addEventListener('DOMContentLoaded', () => {
  const el = document.getElementById('initialTime');
  if (el) el.textContent = timestamp();
});

// ── Status / Log System ─────────────────────────────────────────────────────
function addLog(type, msg) {
  const log = document.getElementById('statusLog');
  const entry = document.createElement('div');
  entry.className = 'log-entry ' + type;
  entry.innerHTML = `<span class="log-time">${timestamp()}</span><span class="log-msg">${msg}</span>`;
  log.appendChild(entry);
  // Auto-scroll to bottom
  log.scrollTop = log.scrollHeight;
}

function clearLog() {
  const log = document.getElementById('statusLog');
  log.innerHTML = '';
  addLog('info', 'Log cleared — ready for new exports.');
  setProgress(false);
}

function setProgress(active, value) {
  const wrap = document.getElementById('progressWrap');
  const bar = document.getElementById('progressBar');

  if (!active) {
    wrap.classList.remove('active');
    bar.classList.remove('indeterminate');
    bar.style.width = '0%';
    return;
  }

  wrap.classList.add('active');

  if (value === undefined || value === null) {
    // Indeterminate
    bar.classList.add('indeterminate');
    bar.style.width = '30%';
  } else {
    bar.classList.remove('indeterminate');
    bar.style.width = Math.min(100, Math.max(0, value)) + '%';
  }
}

function formatBytes(n) {
  if (n < 1024)       return n + ' B';
  if (n < 1048576)    return (n / 1024).toFixed(1) + ' KB';
  if (n < 1073741824) return (n / 1048576).toFixed(2) + ' MB';
  return (n / 1073741824).toFixed(2) + ' GB';
}

// ── Card State Management ───────────────────────────────────────────────────
function setCardActive(cardId, active) {
  const card = cardId ? document.getElementById(cardId) : null;
  if (card) {
    if (active) {
      card.classList.add('active-download');
    } else {
      card.classList.remove('active-download');
      card.classList.add('success-flash');
      setTimeout(() => card.classList.remove('success-flash'), 1000);
    }
  }
}

function setBtnLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.disabled = loading;
  if (loading) {
    btn.classList.add('loading');
  } else {
    btn.classList.remove('loading');
  }
}

// ── Generic: Binary File Download (Stream / Bytes / Excel Stream) ────────────
async function downloadBinaryFile(apiUrl, defaultFileName, btnId, cardId, label) {
  setBtnLoading(btnId, true);
  setCardActive(cardId, true);
  setProgress(true);
  addLog('loading', `⏳ <strong>${label}</strong> — Generating on server…`);

  const startTime = performance.now();

  try {
    const res = await fetch(apiUrl);
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);

    addLog('loading', `⬇ <strong>${label}</strong> — Downloading…`);

    const blob = await res.blob();
    const cd   = res.headers.get('Content-Disposition') || '';
    const m    = cd.match(/filename[^;=\n]*=["']?([^"';\n]+)/i);
    const fileName = m ? m[1].trim() : defaultFileName;

    triggerDownload(blob, fileName);

    const elapsed = ((performance.now() - startTime) / 1000).toFixed(1);
    setProgress(true, 100);
    addLog('success', `✅ <strong>${label}</strong> — ${formatBytes(blob.size)} downloaded as <em>${fileName}</em> in ${elapsed}s`);

    setTimeout(() => setProgress(false), 2000);
  } catch (e) {
    setProgress(false);
    addLog('error', `❌ <strong>${label}</strong> — ${e.message}`);
    console.error(e);
  } finally {
    setBtnLoading(btnId, false);
    setCardActive(cardId, false);
  }
}

// ── Generic: Base64 JSON Download ────────────────────────────────────────────
async function downloadBase64File(apiUrl, defaultFileName, mimeType, btnId, cardId, label) {
  setBtnLoading(btnId, true);
  setCardActive(cardId, true);
  setProgress(true);
  addLog('loading', `⏳ <strong>${label}</strong> — Generating Base64 on server…`);

  const startTime = performance.now();

  try {
    const res = await fetch(apiUrl);
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);

    addLog('loading', `🔄 <strong>${label}</strong> — Decoding Base64 in browser…`);

    const json   = await res.json();
    const binary = atob(json.data);
    const buf    = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) buf[i] = binary.charCodeAt(i);
    const blob   = new Blob([buf], { type: mimeType });
    const fileName = json.fileName || defaultFileName;

    triggerDownload(blob, fileName);

    const elapsed = ((performance.now() - startTime) / 1000).toFixed(1);
    setProgress(true, 100);
    addLog('success', `✅ <strong>${label}</strong> — ${formatBytes(blob.size)} decoded and saved as <em>${fileName}</em> in ${elapsed}s`);

    setTimeout(() => setProgress(false), 2000);
  } catch (e) {
    setProgress(false);
    addLog('error', `❌ <strong>${label}</strong> — ${e.message}`);
    console.error(e);
  } finally {
    setBtnLoading(btnId, false);
    setCardActive(cardId, false);
  }
}

// ── Generic: Server File Save ────────────────────────────────────────────────
async function saveToServerFile(apiUrl, btnId, cardId, label) {
  setBtnLoading(btnId, true);
  setCardActive(cardId, true);
  setProgress(true);
  addLog('loading', `⏳ <strong>${label}</strong> — Writing file on server disk…`);

  const startTime = performance.now();

  try {
    const res = await fetch(apiUrl);
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);

    const json = await res.json();
    const elapsed = ((performance.now() - startTime) / 1000).toFixed(1);

    setProgress(true, 100);
    addLog('success', `✅ <strong>${label}</strong> — ${json.message} in ${elapsed}s<br>&nbsp;&nbsp;&nbsp;&nbsp;📁 <code>${json.path}</code>`);

    setTimeout(() => setProgress(false), 2000);
  } catch (e) {
    setProgress(false);
    addLog('error', `❌ <strong>${label}</strong> — ${e.message}`);
    console.error(e);
  } finally {
    setBtnLoading(btnId, false);
    setCardActive(cardId, false);
  }
}

// =========================================================================
//  CSV — Auto Headers (4 endpoints)
// =========================================================================

function downloadCsvStream() {
  downloadBinaryFile(
    ENDPOINTS.csvStream, 'users.csv',
    'csvStreamBtn', 'card-csvStream',
    'CSV Stream'
  );
}

function downloadCsvBytes() {
  downloadBinaryFile(
    ENDPOINTS.csvBytes, 'users.csv',
    'csvBytesBtn', 'card-csvBytes',
    'CSV Bytes'
  );
}

function downloadCsvBase64() {
  downloadBase64File(
    ENDPOINTS.csvBase64, 'users.csv', 'text/csv',
    'csvBase64Btn', 'card-csvBase64',
    'CSV Base64'
  );
}

function saveCsvToFile() {
  saveToServerFile(
    ENDPOINTS.csvFile,
    'csvFileBtn', 'card-csvFile',
    'CSV Server File'
  );
}

// =========================================================================
//  CSV — Custom Headers (4 endpoints)
// =========================================================================

function downloadCsvCustomHeaders() {
  downloadBinaryFile(
    ENDPOINTS.csvCustom, 'users_custom.csv',
    'csvCustomBtn', 'card-csvCustomStream',
    'CSV Custom Stream'
  );
}

function downloadCsvCustomHeadersBytes() {
  downloadBinaryFile(
    ENDPOINTS.csvCustomBytes, 'users_custom.csv',
    'csvCustomBytesBtn', 'card-csvCustomBytes',
    'CSV Custom Bytes'
  );
}

function downloadCsvCustomHeadersBase64() {
  downloadBase64File(
    ENDPOINTS.csvCustomBase64, 'users_custom.csv', 'text/csv',
    'csvCustomBase64Btn', 'card-csvCustomBase64',
    'CSV Custom Base64'
  );
}

function saveCsvCustomHeadersToFile() {
  saveToServerFile(
    ENDPOINTS.csvCustomFile,
    'csvCustomFileBtn', 'card-csvCustomFile',
    'CSV Custom Server File'
  );
}

// =========================================================================
//  Excel — Auto Headers (4 endpoints)
// =========================================================================

function downloadExcel() {
  downloadBinaryFile(
    ENDPOINTS.excel, 'users.xlsx',
    'excelBtn', 'card-excelStream',
    'Excel Stream'
  );
}

function downloadExcelBytes() {
  downloadBinaryFile(
    ENDPOINTS.excelBytes, 'users.xlsx',
    'excelBytesBtn', 'card-excelBytes',
    'Excel Bytes'
  );
}

function downloadExcelBase64() {
  downloadBase64File(
    ENDPOINTS.excelBase64, 'users.xlsx',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'excelBase64Btn', 'card-excelBase64',
    'Excel Base64'
  );
}

function saveExcelToFile() {
  saveToServerFile(
    ENDPOINTS.excelFile,
    'excelFileBtn', 'card-excelFile',
    'Excel Server File'
  );
}

// =========================================================================
//  Excel — Custom Headers (4 endpoints)
// =========================================================================

function downloadExcelCustomHeaders() {
  downloadBinaryFile(
    ENDPOINTS.excelCustom, 'users_custom.xlsx',
    'excelCustomBtn', 'card-excelCustomStream',
    'Excel Custom Stream'
  );
}

function downloadExcelCustomHeadersBytes() {
  downloadBinaryFile(
    ENDPOINTS.excelCustomBytes, 'users_custom.xlsx',
    'excelCustomBytesBtn', 'card-excelCustomBytes',
    'Excel Custom Bytes'
  );
}

function downloadExcelCustomHeadersBase64() {
  downloadBase64File(
    ENDPOINTS.excelCustomBase64, 'users_custom.xlsx',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'excelCustomBase64Btn', 'card-excelCustomBase64',
    'Excel Custom Base64'
  );
}

function saveExcelCustomHeadersToFile() {
  saveToServerFile(
    ENDPOINTS.excelCustomFile,
    'excelCustomFileBtn', 'card-excelCustomFile',
    'Excel Custom Server File'
  );
}

// =========================================================================
//  Utility — Trigger browser file download from Blob
// =========================================================================
function triggerDownload(blob, fileName) {
  const url  = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href     = url;
  link.download = fileName;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url); // free memory immediately
}
