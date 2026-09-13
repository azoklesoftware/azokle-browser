import SwiftUI
import WebKit

public struct WebViewContainer: UIViewRepresentable {
    @ObservedObject var state: BrowserState
    let tabId: UUID

    public func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    public func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()

        // 1. Compile & Apply Native WebKit Declarative Content Blocker Rules
        AdBlockEngine.shared.compileNativeRuleList { ruleList in
            if let ruleList = ruleList, self.state.isShieldActive {
                configuration.userContentController.add(ruleList)
            }
        }

        // 2. Azokle Shield Privacy Scriptlet (Hardware Uniformity, Battery Shield, RFC 8828 WebRTC Local IP Obfuscation)
        let domain = (state.tabs.first(where: { $0.id == tabId })?.url).flatMap { URL(string: $0)?.host } ?? ""
        let defuserScriptSource = AdBlockEngine.shared.getDefuserScriptlet(domain: domain)
        let cosmeticCss = AdBlockEngine.shared.getCosmeticCss(domain: domain)
        
        let jsonCssData = (try? JSONSerialization.data(withJSONObject: [cosmeticCss], options: [])) ?? Data()
        let jsonCssString = String(data: jsonCssData, encoding: .utf8) ?? "[\"\"]"

        let combinedScriptSource = """
        \(defuserScriptSource)

        (function() {
            try {
                var cssArray = \(jsonCssString);
                if (cssArray && cssArray.length > 0 && cssArray[0]) {
                    var style = document.createElement('style');
                    style.type = 'text/css';
                    style.appendChild(document.createTextNode(cssArray[0]));
                    (document.head || document.documentElement).appendChild(style);
                }
            } catch(e) {}
        })();
        """

        let shieldScript = WKUserScript(
            source: combinedScriptSource,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: false
        )
        configuration.userContentController.addUserScript(shieldScript)

        // 3. Preferences & Media Playback
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.allowsInlineMediaPlayback = true

        // 4. Isolated Ephemeral Website Data Store for Private & Tor Modes
        if let currentTab = state.tabs.first(where: { $0.id == tabId }), currentTab.isPrivate || currentTab.isTor {
            configuration.websiteDataStore = WKWebsiteDataStore.nonPersistent()
        }

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.isOpaque = false
        webView.backgroundColor = UIColor(Color(hex: 0x101010))

        context.coordinator.setupObservers(for: webView)

        if let tab = state.tabs.first(where: { $0.id == tabId }),
           let url = URL(string: tab.url) {
            let safeUrl = NavigationSecurity.shared.upgradeToHttps(url: url)
            let request = URLRequest(url: safeUrl)
            webView.load(request)
        }

        return webView
    }

    public func updateUIView(_ webView: WKWebView, context: Context) {
        guard let tab = state.tabs.first(where: { $0.id == tabId }) else { return }

        // Update Desktop / Mobile Mode User-Agent
        if state.desktopMode {
            webView.customUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
        } else {
            webView.customUserAgent = nil
        }

        // Handle pending web navigation actions (Back, Forward, Reload, Stop)
        if let pending = state.pendingAction, pending.0 == tabId {
            switch pending.1 {
            case .goBack:
                if webView.canGoBack {
                    webView.goBack()
                }
            case .goForward:
                if webView.canGoForward {
                    webView.goForward()
                }
            case .reload:
                webView.reload()
            case .stop:
                webView.stopLoading()
            }
            DispatchQueue.main.async {
                self.state.pendingAction = nil
            }
        }

        // Check if URL changed externally (e.g. from omnibox navigation)
        if let currentUrl = webView.url?.absoluteString, currentUrl != tab.url, !tab.isLoading {
            if let newUrl = URL(string: tab.url) {
                let safeUrl = NavigationSecurity.shared.upgradeToHttps(url: newUrl)
                let request = URLRequest(url: safeUrl)
                webView.load(request)
            }
        }
    }

    public class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var parent: WebViewContainer
        private var progressObservation: NSKeyValueObservation?
        private var titleObservation: NSKeyValueObservation?
        private var urlObservation: NSKeyValueObservation?
        private var canGoBackObservation: NSKeyValueObservation?
        private var canGoForwardObservation: NSKeyValueObservation?

        init(_ parent: WebViewContainer) {
            self.parent = parent
        }

        deinit {
            progressObservation?.invalidate()
            titleObservation?.invalidate()
            urlObservation?.invalidate()
            canGoBackObservation?.invalidate()
            canGoForwardObservation?.invalidate()
        }

        func setupObservers(for webView: WKWebView) {
            progressObservation = webView.observe(\.estimatedProgress, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].progress = webView.estimatedProgress
                    }
                }
            }

            titleObservation = webView.observe(\.title, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let title = webView.title, !title.isEmpty,
                       let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].title = title
                    }
                }
            }

            urlObservation = webView.observe(\.url, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let url = webView.url?.absoluteString, !url.isEmpty,
                       let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].url = url
                    }
                }
            }

            canGoBackObservation = webView.observe(\.canGoBack, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].canGoBack = webView.canGoBack
                    }
                }
            }

            canGoForwardObservation = webView.observe(\.canGoForward, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].canGoForward = webView.canGoForward
                    }
                }
            }
        }

        public func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            DispatchQueue.main.async {
                if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                    self.parent.state.tabs[index].isLoading = true
                }
            }
        }

        public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            DispatchQueue.main.async {
                if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                    self.parent.state.tabs[index].isLoading = false
                    self.parent.state.tabs[index].progress = 1.0
                }
            }
        }

        public func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            DispatchQueue.main.async {
                if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                    self.parent.state.tabs[index].isLoading = false
                }
            }
        }

        // Intercept navigation to prevent dangerous schemes and handle system intents via NavigationSecurity
        public func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }

            let currentTab = parent.state.tabs.first(where: { $0.id == parent.tabId })
            let isTor = currentTab?.isTor ?? false
            let decision = NavigationSecurity.shared.evaluateNavigation(url: url, isTor: isTor)

            switch decision {
            case .allow:
                decisionHandler(.allow)
            case .cancel:
                decisionHandler(.cancel)
            case .openExternalApp(let externalUrl):
                DispatchQueue.main.async {
                    UIApplication.shared.open(externalUrl)
                }
                decisionHandler(.cancel)
            case .blockedDangerousScheme(let scheme):
                print("[NavigationSecurity] Blocked dangerous scheme: \(scheme)")
                decisionHandler(.cancel)
            case .blockedTorClearnetLeak(let externalUrl):
                print("[NavigationSecurity] Prevented Tor clearnet leak to external app: \(externalUrl)")
                decisionHandler(.cancel)
            }
        }

        // Handle target="_blank" or window.open in isolated new tab
        public func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            if navigationAction.targetFrame == nil, let url = navigationAction.request.url?.absoluteString {
                let currentTab = parent.state.tabs.first(where: { $0.id == parent.tabId })
                let isPrivate = currentTab?.isPrivate ?? false
                let isTor = currentTab?.isTor ?? false
                DispatchQueue.main.async {
                    self.parent.state.openNewTab(url: url, title: "New Tab", isPrivate: isPrivate, isTor: isTor)
                }
            }
            return nil
        }
    }
}
