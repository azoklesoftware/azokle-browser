// Azokle Shield WebUI Controller
(function() {
  const statAds = document.getElementById('stat-ads');
  const statTrackers = document.getElementById('stat-trackers');
  const statFingerprint = document.getElementById('stat-fingerprint');
  const statBandwidth = document.getElementById('stat-bandwidth');
  const refreshBtn = document.getElementById('refresh-stats');

  function updateMetrics(data) {
    if (!data) return;
    const ads = data.ads || 0;
    const trackers = data.trackers || 0;
    const fingerprinting = data.fingerprinting || 0;
    const total = ads + trackers + fingerprinting;

    statAds.textContent = ads.toLocaleString();
    statTrackers.textContent = trackers.toLocaleString();
    statFingerprint.textContent = fingerprinting.toLocaleString();
    
    // Estimate: ~150KB saved per blocked request
    const mbSaved = ((total * 150) / 1024).toFixed(1);
    statBandwidth.textContent = `${mbSaved} MB`;
  }

  function fetchStats() {
    if (window.azokleNative && typeof window.azokleNative.getShieldStats === 'function') {
      try {
        const stats = JSON.parse(window.azokleNative.getShieldStats());
        updateMetrics(stats);
      } catch (e) {
        console.error('Error fetching stats:', e);
      }
    } else if (window.postMessage) {
      // Fallback mock/simulated metrics for testing and standalone preview
      updateMetrics({
        ads: 42,
        trackers: 87,
        fingerprinting: 14
      });
    }
  }

  if (refreshBtn) {
    refreshBtn.addEventListener('click', fetchStats);
  }

  // Initial load
  fetchStats();
})();
