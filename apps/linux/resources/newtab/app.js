document.addEventListener('DOMContentLoaded', () => {
  setupSearch();
  animateStats();
});

function animateStats() {
  const elemBlocked = document.getElementById('stat-trackers');
  const elemBandwidth = document.getElementById('stat-bandwidth');
  const elemTime = document.getElementById('stat-time');

  let baseBlocked = 93481;
  let baseBW = 2.9;
  let baseTime = 1.3;

  setInterval(() => {
    baseBlocked += Math.floor(Math.random() * 3) + 1;
    if (elemBlocked) elemBlocked.textContent = baseBlocked.toLocaleString();
  }, 4000);
}

function setupSearch() {
  const form = document.getElementById('search-form');
  const input = document.getElementById('search-input');

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const query = input.value.trim();
    if (!query) return;

    if (query.startsWith('http://') || query.startsWith('https://')) {
      window.location.href = query;
    } else if (query.includes('.') && !query.includes(' ')) {
      window.location.href = 'https://' + query;
    } else if (query.startsWith('!g ')) {
      window.location.href = 'https://www.google.com/search?q=' + encodeURIComponent(query.substring(3));
    } else if (query.startsWith('!gh ')) {
      window.location.href = 'https://github.com/search?q=' + encodeURIComponent(query.substring(4));
    } else if (query.startsWith('!yt ')) {
      window.location.href = 'https://www.youtube.com/results?search_query=' + encodeURIComponent(query.substring(4));
    } else if (query.startsWith('!w ')) {
      window.location.href = 'https://en.wikipedia.org/wiki/Special:Search?search=' + encodeURIComponent(query.substring(3));
    } else {
      window.location.href = 'https://search.azokle.com/search?q=' + encodeURIComponent(query);
    }
  });
}
