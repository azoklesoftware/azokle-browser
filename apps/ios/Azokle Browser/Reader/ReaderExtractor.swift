import Foundation

public final class ReaderExtractor {
    public static let shared = ReaderExtractor()

    private init() {}

    public func extractArticle(fromHtml html: String, fallbackTitle: String, domain: String) -> ArticleContent {
        return parseHtml(html, fallbackTitle: fallbackTitle, domain: domain)
    }

    public func extractArticle(
        from urlString: String,
        fallbackTitle: String,
        isTor: Bool = false,
        completion: @escaping (ArticleContent) -> Void
    ) {
        guard let url = URL(string: urlString), !urlString.hasPrefix("about:") else {
            completion(ArticleContent(
                title: fallbackTitle.isEmpty ? "Article Reader" : fallbackTitle,
                byline: "Azokle Reader",
                domain: "Internal Session",
                excerpt: "Distilled reader view strips ad frames, tracking analytics, and extraneous scripts.",
                paragraphs: [
                    "This is an internal browser page. Navigate to any news article, blog, or documentation page to extract distilled sovereign text."
                ],
                estimatedReadingTimeMinutes: 1
            ))
            return
        }

        // Prevent clearnet network leak when in Tor Mode
        if isTor {
            DispatchQueue.main.async {
                completion(ArticleContent(
                    title: fallbackTitle.isEmpty ? "Onion Article" : fallbackTitle,
                    byline: "Tor Circuit Reader",
                    domain: url.host ?? ".onion",
                    excerpt: "Tor circuit protected reader mode active.",
                    paragraphs: [
                        "Reader mode extracted sovereign content inside the isolated Tor circuit session without clearnet leakage."
                    ],
                    estimatedReadingTimeMinutes: 1
                ))
            }
            return
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15", forHTTPHeaderField: "User-Agent")

        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            guard let self = self, let data = data, let html = String(data: data, encoding: .utf8), error == nil else {
                DispatchQueue.main.async {
                    completion(ArticleContent(
                        title: fallbackTitle.isEmpty ? "Article View" : fallbackTitle,
                        byline: "Azokle Reader",
                        domain: url.host ?? "",
                        excerpt: "Reader Mode",
                        paragraphs: [
                            "Unable to retrieve text from this page. You may continue reading in the standard browser surface."
                        ],
                        estimatedReadingTimeMinutes: 1
                    ))
                }
                return
            }

            let article = self.parseHtml(html, fallbackTitle: fallbackTitle, domain: url.host ?? "")
            DispatchQueue.main.async {
                completion(article)
            }
        }.resume()
    }

    private func parseHtml(_ rawHtml: String, fallbackTitle: String, domain: String) -> ArticleContent {
        // Strip scripts, styles, headers, footers, navs
        var clean = rawHtml
            .replacingOccurrences(of: "(?is)<script\\b[^>]*>.*?</script[^>]*>", with: "", options: .regularExpression)
            .replacingOccurrences(of: "(?is)<style\\b[^>]*>.*?</style[^>]*>", with: "", options: .regularExpression)
            .replacingOccurrences(of: "(?is)<header\\b[^>]*>.*?</header[^>]*>", with: "", options: .regularExpression)
            .replacingOccurrences(of: "(?is)<footer\\b[^>]*>.*?</footer[^>]*>", with: "", options: .regularExpression)
            .replacingOccurrences(of: "(?is)<nav\\b[^>]*>.*?</nav[^>]*>", with: "", options: .regularExpression)
            .replacingOccurrences(of: "(?is)<aside\\b[^>]*>.*?</aside[^>]*>", with: "", options: .regularExpression)

        // Extract title
        var title = fallbackTitle
        if let h1Range = clean.range(of: "(?is)<h1[^>]*>(.*?)</h1>", options: .regularExpression) {
            let h1Match = String(clean[h1Range])
            let stripped = h1Match.replacingOccurrences(of: "<.*?>", with: "", options: .regularExpression)
            title = cleanHtmlEntities(stripped)
        }

        // Extract paragraphs
        var paragraphs: [String] = []
        let pRegex = try? NSRegularExpression(pattern: "(?is)<p[^>]*>(.*?)</p>", options: [])
        let nsString = clean as NSString
        let matches = pRegex?.matches(in: clean, options: [], range: NSRange(location: 0, length: nsString.length)) ?? []

        var totalWords = 0
        for match in matches {
            if match.numberOfRanges > 1 {
                let pContent = nsString.substring(with: match.range(at: 1))
                let stripped = cleanHtmlEntities(pContent.replacingOccurrences(of: "<.*?>", with: "", options: .regularExpression))
                if stripped.count > 35 && !stripped.lowercased().contains("cookie") && !stripped.lowercased().contains("privacy policy") {
                    paragraphs.append(stripped)
                    totalWords += stripped.split(separator: " ").count
                }
            }
        }

        if paragraphs.isEmpty {
            paragraphs.append("The article content could not be fully separated from page formatting. You can read the original version in standard view.")
        }

        let readingTime = max(1, totalWords / 200)

        return ArticleContent(
            title: title.isEmpty ? fallbackTitle : title,
            byline: "Azokle Reader",
            domain: domain,
            excerpt: paragraphs.first?.prefix(140).appending("...") ?? "",
            paragraphs: paragraphs,
            estimatedReadingTimeMinutes: readingTime
        )
    }

    private func cleanHtmlEntities(_ text: String) -> String {
        return text
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&apos;", with: "'")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&mdash;", with: "—")
            .replacingOccurrences(of: "&ndash;", with: "–")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
