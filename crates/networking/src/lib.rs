use serde::{Deserialize, Serialize};
use std::process::Command;
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ParsedWebPage {
    pub url: String,
    pub title: String,
    pub status_code: u16,
    pub headers: Vec<String>,
    pub headings: Vec<String>,
    pub paragraphs: Vec<String>,
    pub links: Vec<(String, String)>, // (label, url)
    pub raw_html_snippet: String,
}

pub struct HttpClient;

impl HttpClient {
    pub fn fetch_url(url: &str, is_tor: bool) -> Result<ParsedWebPage, String> {
        let mut cmd = Command::new("curl");
        cmd.arg("-sL")
            .arg("-m")
            .arg("10")
            .arg("-A")
            .arg("Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0 (Azokle Sovereign)");

        if is_tor {
            cmd.arg("--socks5-hostname").arg("127.0.0.1:9050");
        }

        cmd.arg(url);

        let output = cmd.output().map_err(|e| format!("Failed to execute network request: {}", e))?;
        if !output.status.success() && output.stdout.is_empty() {
            return Err("Network request timed out or host unreachable".to_string());
        }

        let body = String::from_utf8_lossy(&output.stdout).to_string();
        Ok(Self::parse_html(url, &body))
    }

    pub fn parse_html(url: &str, html: &str) -> ParsedWebPage {
        let mut title = "Web Document".to_string();
        if let Some(start) = html.find("<title>") {
            if let Some(end) = html[start + 7..].find("</title>") {
                title = html[start + 7..start + 7 + end].trim().to_string();
            }
        } else if let Some(start) = html.find("<TITLE>") {
            if let Some(end) = html[start + 7..].find("</TITLE>") {
                title = html[start + 7..start + 7 + end].trim().to_string();
            }
        }

        let mut headings = Vec::new();
        let mut paragraphs = Vec::new();
        let mut links = Vec::new();

        // Extract H1 / H2
        for tag in &["<h1", "<h2", "<H1", "<H2"] {
            let mut pos = 0;
            while let Some(idx) = html[pos..].find(tag) {
                let actual_idx = pos + idx;
                if let Some(close_tag) = html[actual_idx..].find('>') {
                    let text_start = actual_idx + close_tag + 1;
                    if let Some(end_tag) = html[text_start..].find('<') {
                        let text = html[text_start..text_start + end_tag].trim();
                        if !text.is_empty() && text.len() < 120 {
                            headings.push(Self::clean_text(text));
                        }
                    }
                }
                pos = actual_idx + 4;
                if headings.len() >= 6 {
                    break;
                }
            }
        }

        // Extract Paragraphs / Text Blocks
        let mut pos = 0;
        while let Some(idx) = html[pos..].find("<p") {
            let actual_idx = pos + idx;
            if let Some(close_tag) = html[actual_idx..].find('>') {
                let text_start = actual_idx + close_tag + 1;
                if let Some(end_tag) = html[text_start..].find("</p>") {
                    let text = html[text_start..text_start + end_tag].trim();
                    let clean = Self::clean_text(text);
                    if clean.len() > 15 {
                        paragraphs.push(clean);
                    }
                }
            }
            pos = actual_idx + 3;
            if paragraphs.len() >= 8 {
                break;
            }
        }

        // Extract Links <a href="...">
        let mut pos = 0;
        while let Some(idx) = html[pos..].find("<a ") {
            let actual_idx = pos + idx;
            if let Some(href_pos) = html[actual_idx..].find("href=\"") {
                let val_start = actual_idx + href_pos + 6;
                if let Some(val_end) = html[val_start..].find('\"') {
                    let href = html[val_start..val_start + val_end].trim();
                    if let Some(tag_close) = html[val_start + val_end..].find('>') {
                        let label_start = val_start + val_end + tag_close + 1;
                        if let Some(label_end) = html[label_start..].find("</a>") {
                            let label = html[label_start..label_start + label_end].trim();
                            let clean_label = Self::clean_text(label);
                            if !clean_label.is_empty() && !href.starts_with('#') && !href.starts_with("javascript:") {
                                links.push((clean_label, href.to_string()));
                            }
                        }
                    }
                }
            }
            pos = actual_idx + 4;
            if links.len() >= 8 {
                break;
            }
        }

        let snippet = if html.len() > 1000 {
            html[..1000].to_string()
        } else {
            html.to_string()
        };

        ParsedWebPage {
            url: url.to_string(),
            title,
            status_code: 200,
            headers: vec![],
            headings,
            paragraphs,
            links,
            raw_html_snippet: snippet,
        }
    }

    fn clean_text(input: &str) -> String {
        let mut out = String::new();
        let mut in_tag = false;
        for c in input.chars() {
            if c == '<' {
                in_tag = true;
            } else if c == '>' {
                in_tag = false;
            } else if !in_tag {
                out.push(c);
            }
        }
        out.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .split_whitespace()
            .collect::<Vec<_>>()
            .join(" ")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_html_parsing() {
        let html = r#"
        <!DOCTYPE html>
        <html>
        <head><title>Azokle Search</title></head>
        <body>
            <h1>Private Sovereign Search</h1>
            <p>Welcome to Azokle, the world's most private and sovereign search experience.</p>
            <a href="https://search.azokle.com/settings">Settings</a>
        </body>
        </html>
        "#;

        let parsed = HttpClient::parse_html("https://search.azokle.com", html);
        assert_eq!(parsed.title, "Azokle Search");
        assert_eq!(parsed.headings.len(), 1);
        assert_eq!(parsed.headings[0], "Private Sovereign Search");
        assert_eq!(parsed.paragraphs.len(), 1);
        assert_eq!(parsed.links.len(), 1);
        assert_eq!(parsed.links[0].0, "Settings");
    }
}
