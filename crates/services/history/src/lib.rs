use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, VecDeque};
use std::sync::Arc;
use url::Url;

/// Computes the Firefox/Chromium-grade Frecency (Frequency + Recency) score for a history entry.
///
/// Formula:
/// - Recency Multiplier:
///   - 0..=4 days: 100%
///   - 5..=14 days: 70%
///   - 15..=31 days: 50%
///   - 32..=90 days: 30%
///   - > 90 days: 10%
/// - Type Bonus:
///   - Typed visits: 2,000 points per typed navigation
///   - Standard visits: 1,000 points per visit
pub fn compute_frecency(visit_count: u32, typed_count: u32, last_visit_ms: u64, now_ms: u64) -> i64 {
    let delta_ms = now_ms.saturating_sub(last_visit_ms);
    let delta_days = delta_ms / (1000 * 60 * 60 * 24);
    let recency_multiplier: i64 = match delta_days {
        0..=4 => 100,
        5..=14 => 70,
        15..=31 => 50,
        32..=90 => 30,
        _ => 10,
    };
    let type_bonus = (typed_count as i64 * 2000) + (visit_count.max(1) as i64 * 1000);
    (type_bonus * recency_multiplier) / 100
}

/// Record representing a visited web page.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryEntry {
    /// Unique visit entry identifier formatted as `hist_{timestamp}_{index}`.
    pub id: String,
    /// Page title recorded at navigation or updated upon document parse.
    pub title: String,
    /// Canonical page URL.
    pub url: String,
    /// Millisecond Unix timestamp of the most recent visit.
    pub visit_time: u64,
    /// Total cumulative number of visits to this URL.
    pub visit_count: u32,
    /// Total number of times the URL was explicitly typed in the address bar.
    pub typed_count: u32,
    /// Computed Frecency score for intelligent ranking & autocompletion.
    pub frecency: i64,
    /// Hostname / domain component extracted from the URL.
    pub domain: String,
    /// Indicates whether the URL is also marked in the bookmarks collection.
    pub is_bookmarked: bool,
}

/// Aggregated domain statistic for quick filtering.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DomainStat {
    pub domain: String,
    pub count: usize,
}

/// Chronological timeline grouping for UI display.
#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct GroupedTimeline {
    pub today: Vec<HistoryEntry>,
    pub yesterday: Vec<HistoryEntry>,
    pub last_7_days: Vec<HistoryEntry>,
    pub last_30_days: Vec<HistoryEntry>,
    pub older: Vec<HistoryEntry>,
}

/// Thread-safe manager for browsing history entries, query search, frecency scoring, and privacy sanitization.
#[derive(Clone)]
pub struct HistoryManager {
    entries: Arc<RwLock<VecDeque<HistoryEntry>>>,
    max_entries: usize,
}

impl Default for HistoryManager {
    fn default() -> Self {
        Self::new()
    }
}

impl HistoryManager {
    /// Creates a history manager with a default capacity of 50,000 entries.
    pub fn new() -> Self {
        Self {
            entries: Arc::new(RwLock::new(VecDeque::new())),
            max_entries: 50_000,
        }
    }

    /// Creates a history manager with a custom maximum entry capacity.
    pub fn with_capacity(max_entries: usize) -> Self {
        Self {
            entries: Arc::new(RwLock::new(VecDeque::new())),
            max_entries,
        }
    }

    /// Records a standard page visit.
    pub fn record_visit(&self, title: &str, url_str: &str, is_private: bool) -> Option<HistoryEntry> {
        self.record_visit_internal(title, url_str, is_private, false)
    }

    /// Records a typed visit (explicit user omnibox input) with enhanced frecency weight.
    pub fn record_typed_visit(&self, title: &str, url_str: &str, is_private: bool) -> Option<HistoryEntry> {
        self.record_visit_internal(title, url_str, is_private, true)
    }

    fn record_visit_internal(&self, title: &str, url_str: &str, is_private: bool, is_typed: bool) -> Option<HistoryEntry> {
        if is_private || url_str.trim().is_empty() {
            return None;
        }

        let clean_url = url_str.trim();
        if clean_url.starts_with("about:")
            || clean_url.starts_with("chrome:")
            || clean_url.starts_with("javascript:")
            || clean_url.starts_with("data:")
            || clean_url.starts_with("blob:")
            || clean_url.starts_with("resource:")
        {
            return None;
        }

        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let domain = Url::parse(clean_url)
            .ok()
            .and_then(|u| u.host_str().map(|h| h.to_string()))
            .unwrap_or_default();

        let clean_title = if title.trim().is_empty() {
            clean_url.to_string()
        } else {
            title.trim().to_string()
        };

        let mut list = self.entries.write();
        if let Some(pos) = list.iter().position(|e| e.url == clean_url) {
            let mut existing = list.remove(pos).unwrap();
            existing.visit_count += 1;
            if is_typed {
                existing.typed_count += 1;
            }
            existing.visit_time = now_ms;
            if !title.trim().is_empty() {
                existing.title = clean_title;
            }
            existing.frecency = compute_frecency(existing.visit_count, existing.typed_count, existing.visit_time, now_ms);
            list.push_front(existing.clone());
            Some(existing)
        } else {
            let id = format!("hist_{}_{}", now_ms, list.len());
            let typed_count = if is_typed { 1 } else { 0 };
            let frecency = compute_frecency(1, typed_count, now_ms, now_ms);
            let entry = HistoryEntry {
                id,
                title: clean_title,
                url: clean_url.to_string(),
                visit_time: now_ms,
                visit_count: 1,
                typed_count,
                frecency,
                domain,
                is_bookmarked: false,
            };

            list.push_front(entry.clone());
            if list.len() > self.max_entries {
                list.pop_back();
            }
            Some(entry)
        }
    }

    /// Searches history entries matching the query in title, URL, or domain.
    pub fn search(&self, query: &str, limit: usize) -> Vec<HistoryEntry> {
        let q = query.trim().to_lowercase();
        if q.is_empty() {
            return self.get_recent(limit, 0);
        }

        self.entries
            .read()
            .iter()
            .filter(|e| {
                e.title.to_lowercase().contains(&q)
                    || e.url.to_lowercase().contains(&q)
                    || e.domain.to_lowercase().contains(&q)
            })
            .take(limit.max(1))
            .cloned()
            .collect()
    }

    /// Performs search and ranks matching results by Frecency score descending (ideal for Omnibox autocompletion).
    pub fn search_frecency(&self, query: &str, limit: usize) -> Vec<HistoryEntry> {
        let q = query.trim().to_lowercase();
        let guard = self.entries.read();

        let mut matches: Vec<HistoryEntry> = if q.is_empty() {
            guard.iter().cloned().collect()
        } else {
            guard
                .iter()
                .filter(|e| {
                    e.title.to_lowercase().contains(&q)
                        || e.url.to_lowercase().contains(&q)
                        || e.domain.to_lowercase().contains(&q)
                })
                .cloned()
                .collect()
        };

        matches.sort_by(|a, b| b.frecency.cmp(&a.frecency));
        matches.into_iter().take(limit.max(1)).collect()
    }

    /// Returns top destinations sorted by Frecency score for the New Tab Page and Omnibox top sites.
    pub fn get_top_frecency(&self, limit: usize) -> Vec<HistoryEntry> {
        let mut list: Vec<HistoryEntry> = self.entries.read().iter().cloned().collect();
        list.sort_by(|a, b| b.frecency.cmp(&a.frecency));
        list.into_iter().take(limit.max(1)).collect()
    }

    /// Returns top visited domains and their frequencies.
    pub fn get_top_domains(&self, limit: usize) -> Vec<DomainStat> {
        let mut counts: HashMap<String, usize> = HashMap::new();
        for entry in self.entries.read().iter() {
            if !entry.domain.is_empty() {
                *counts.entry(entry.domain.clone()).or_insert(0) += 1;
            }
        }

        let mut stats: Vec<DomainStat> = counts
            .into_iter()
            .map(|(domain, count)| DomainStat { domain, count })
            .collect();
        stats.sort_by(|a, b| b.count.cmp(&a.count));
        stats.into_iter().take(limit.max(1)).collect()
    }

    /// Groups the entire browsing history into distinct chronological timeline buckets.
    pub fn get_grouped_timeline(&self) -> GroupedTimeline {
        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let day_ms = 86_400_000u64;
        let today_start = now_ms.saturating_sub(now_ms % day_ms);
        let yesterday_start = today_start.saturating_sub(day_ms);
        let last_7_days_start = today_start.saturating_sub(6 * day_ms);
        let last_30_days_start = today_start.saturating_sub(29 * day_ms);

        let mut timeline = GroupedTimeline::default();
        for entry in self.entries.read().iter() {
            if entry.visit_time >= today_start {
                timeline.today.push(entry.clone());
            } else if entry.visit_time >= yesterday_start {
                timeline.yesterday.push(entry.clone());
            } else if entry.visit_time >= last_7_days_start {
                timeline.last_7_days.push(entry.clone());
            } else if entry.visit_time >= last_30_days_start {
                timeline.last_30_days.push(entry.clone());
            } else {
                timeline.older.push(entry.clone());
            }
        }
        timeline
    }

    pub fn get_recent(&self, limit: usize, offset: usize) -> Vec<HistoryEntry> {
        self.entries
            .read()
            .iter()
            .skip(offset)
            .take(limit)
            .cloned()
            .collect()
    }

    pub fn delete_entry(&self, id: &str) -> bool {
        let mut list = self.entries.write();
        if let Some(pos) = list.iter().position(|e| e.id == id) {
            list.remove(pos);
            true
        } else {
            false
        }
    }

    pub fn delete_url(&self, url_str: &str) -> bool {
        let mut list = self.entries.write();
        let initial_len = list.len();
        list.retain(|e| e.url != url_str);
        list.len() < initial_len
    }

    pub fn delete_domain(&self, domain: &str) -> usize {
        let d_clean = domain.trim().to_lowercase();
        let mut list = self.entries.write();
        let initial_len = list.len();
        list.retain(|e| e.domain.to_lowercase() != d_clean);
        initial_len - list.len()
    }

    pub fn clear_time_range(&self, from_ms: u64, to_ms: u64) -> usize {
        let mut list = self.entries.write();
        let initial_len = list.len();
        list.retain(|e| e.visit_time < from_ms || e.visit_time > to_ms);
        initial_len - list.len()
    }

    pub fn clear_all(&self) {
        self.entries.write().clear();
    }

    pub fn total_count(&self) -> usize {
        self.entries.read().len()
    }

    pub fn export_json(&self) -> String {
        serde_json::to_string(&*self.entries.read()).unwrap_or_else(|_| "[]".to_string())
    }

    pub fn import_json(&self, json_str: &str) -> usize {
        if json_str.trim().is_empty() {
            return 0;
        }

        let imported: VecDeque<HistoryEntry> = match serde_json::from_str(json_str) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        let count = imported.len();
        let mut list = self.entries.write();
        *list = imported;
        count
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_frecency_calculation() {
        let now = 1_000_000_000u64;
        // Visited today, 1 visit, 0 typed
        let score_recent = compute_frecency(1, 0, now, now);
        assert_eq!(score_recent, 1000);

        // Visited today, 1 typed visit
        let score_typed = compute_frecency(1, 1, now, now);
        assert_eq!(score_typed, 3000);

        // Visited 10 days ago (multiplier 70%)
        let ten_days_ago = now - (10 * 86_400_000);
        let score_old = compute_frecency(1, 0, ten_days_ago, now);
        assert_eq!(score_old, 700);
    }

    #[test]
    fn test_frecency_ranking_and_search() {
        let history = HistoryManager::new();
        history.record_visit("Azokle Search", "https://search.azokle.com", false);
        history.record_typed_visit("Azokle Docs", "https://docs.azokle.com", false);

        let top = history.get_top_frecency(5);
        assert_eq!(top.len(), 2);
        // Typed visit must have higher frecency and rank first
        assert_eq!(top[0].url, "https://docs.azokle.com");

        let search_res = history.search_frecency("azokle", 5);
        assert_eq!(search_res.len(), 2);
        assert_eq!(search_res[0].url, "https://docs.azokle.com");
    }

    #[test]
    fn test_top_domains_and_grouping() {
        let history = HistoryManager::new();
        history.record_visit("Github 1", "https://github.com/azokle/browser", false);
        history.record_visit("Github 2", "https://github.com/azokle/search", false);
        history.record_visit("Wikipedia", "https://wikipedia.org/wiki/Rust", false);

        let domains = history.get_top_domains(5);
        assert_eq!(domains.len(), 2);
        assert_eq!(domains[0].domain, "github.com");
        assert_eq!(domains[0].count, 2);

        let timeline = history.get_grouped_timeline();
        assert_eq!(timeline.today.len(), 3);
    }

    #[test]
    fn test_history_lifecycle() {
        let history = HistoryManager::new();

        // 1. Regular recording
        let entry = history.record_visit("Azokle Search", "https://search.azokle.com", false);
        assert!(entry.is_some());
        assert_eq!(history.total_count(), 1);

        // 2. Private tabs are ignored
        let private_entry = history.record_visit("Secret", "https://secret.com", true);
        assert!(private_entry.is_none());
        assert_eq!(history.total_count(), 1);

        // 3. Increment visit count on revisit
        history.record_visit("Azokle Search", "https://search.azokle.com", false);
        let recent = history.get_recent(10, 0);
        assert_eq!(recent[0].visit_count, 2);

        // 4. Search
        let results = history.search("search", 5);
        assert_eq!(results.len(), 1);

        // 5. Delete by URL
        assert!(history.delete_url("https://search.azokle.com"));
        assert_eq!(history.total_count(), 0);
    }

    #[test]
    fn test_time_range_clearing() {
        let history = HistoryManager::new();
        history.record_visit("Page 1", "https://page1.com", false);
        history.record_visit("Page 2", "https://page2.com", false);

        assert_eq!(history.total_count(), 2);
        let cleared = history.clear_time_range(0, u64::MAX);
        assert_eq!(cleared, 2);
        assert_eq!(history.total_count(), 0);
    }
}
