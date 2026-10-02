// Azokle Shield Content Engine
// High-Performance Declarative Cosmetic Filtering & Top-Level PWA Bridge

(function() {
    'use strict';

    if (window.__azokleShieldExtLoaded) return;
    window.__azokleShieldExtLoaded = true;

    var hostName = (window.location && window.location.hostname ? window.location.hostname : '').toLowerCase();

    // Sovereign & Benchmark exemption: Never inject on search engine, Cloudflare challenge, or benchmark/local pages
    if (!hostName ||
        hostName === 'azokle.com' || hostName.endsWith('.azokle.com') ||
        hostName === 'cloudflare.com' || hostName.endsWith('.cloudflare.com') ||
        hostName.includes('challenges.cloudflare.com') ||
        hostName.includes('browserbench.org') ||
        hostName === 'localhost' || hostName === '127.0.0.1') {
        return;
    }

    // =========================================================================
    // 1. Compiled Scriptlets & Procedural Defusers Injection (GET_SCRIPTLETS)
    // Injected directly into the Main Realm at document_start
    // =========================================================================
    function injectScriptletIntoPage(code) {
        if (!code || typeof code !== 'string' || code.trim().length === 0) return;
        try {
            var script = document.createElement('script');
            script.textContent = code;

            // Inherit CSP nonce from existing script if present
            var nonceHolder = document.querySelector('script[nonce]');
            if (nonceHolder) {
                var n = nonceHolder.nonce || nonceHolder.getAttribute('nonce');
                if (n) script.setAttribute('nonce', n);
            }

            var target = document.head || document.documentElement || document;
            if (target) {
                target.appendChild(script);
                script.remove();
            } else {
                var obs = new MutationObserver(function() {
                    var t = document.head || document.documentElement || document;
                    if (t) {
                        obs.disconnect();
                        t.appendChild(script);
                        script.remove();
                    }
                });
                obs.observe(document, { childList: true, subtree: true });
            }
        } catch(e) {}
    }

    try {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
            browser.runtime.sendMessage({
                type: 'GET_SCRIPTLETS',
                domain: hostName
            }).then(function(res) {
                if (res && res.scriptlet) {
                    injectScriptletIntoPage(res.scriptlet);
                }
            }).catch(function() {});
        }
    } catch(e) {}

    // =========================================================================
    // 2. Declarative Cosmetic CSS & Procedural DOM Filter Engine
    // Injected into both top-level and subframes to annihilate nested ad containers
    // =========================================================================
    var cosmeticApplied = false;
    function applyCosmeticStyles(css) {
        if (!css || typeof css !== 'string' || css.trim().length === 0 || cosmeticApplied) return;
        try {
            var existing = document.getElementById('azokle-shield-cosmetic');
            if (!existing) {
                var styleEl = document.createElement('style');
                styleEl.id = 'azokle-shield-cosmetic';
                styleEl.textContent = css;
                (document.head || document.documentElement || document).appendChild(styleEl);
                cosmeticApplied = true;
            }
        } catch(e) {}
    }

    // Procedural DOM Filter Engine (Throttled & Non-Blocking)
    function applyProceduralFilters() {
        if (!document || !document.body) return;
        try {
            var adBadges = document.querySelectorAll('[aria-label*="sponsored" i], [aria-label*="advertisement" i], [data-ad-slot], [data-ad-client]');
            for (var i = 0; i < adBadges.length; i++) {
                var el = adBadges[i];
                if (el && el.style.display !== 'none') {
                    var container = el.closest('div, section, article, aside') || el;
                    container.style.setProperty('display', 'none', 'important');
                }
            }
        } catch(e) {}
    }

    var proceduralTimer = null;
    function scheduleProceduralPass() {
        if (proceduralTimer) return;
        var runner = window.requestIdleCallback || window.requestAnimationFrame || function(cb) { setTimeout(cb, 120); };
        proceduralTimer = runner(function() {
            proceduralTimer = null;
            applyProceduralFilters();
        });
    }

    if (document.body) {
        scheduleProceduralPass();
    } else {
        document.addEventListener('DOMContentLoaded', scheduleProceduralPass, { once: true, passive: true });
    }

    try {
        var domObserver = new MutationObserver(function(mutations) {
            var hasRelevantNodes = false;
            for (var i = 0; i < mutations.length; i++) {
                if (mutations[i].addedNodes && mutations[i].addedNodes.length > 0) {
                    hasRelevantNodes = true;
                    break;
                }
            }
            if (hasRelevantNodes) {
                scheduleProceduralPass();
            }
        });
        domObserver.observe(document.documentElement || document, { childList: true, subtree: true });
    } catch(e) {}

    try {
        if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
            browser.runtime.sendMessage({
                type: 'GET_COSMETIC_CSS',
                domain: hostName
            }).then(function(res) {
                if (res && res.css) {
                    applyCosmeticStyles(res.css);
                }
            }).catch(function() {});
        }
    } catch(e) {}

    // =========================================================================
    // 3. W3C Web App Manifest & PWA Scanner (Single Top-Level Dispatch on Load)
    // =========================================================================
    if (window.self !== window.top) return;

    var manifestDispatched = false;
    var scanForManifest = function() {
        if (manifestDispatched) return;
        manifestDispatched = true;

        try {
            var manifestLink = document.querySelector('link[rel="manifest"]');
            var iconLinks = Array.from(document.querySelectorAll('link[rel~="icon"], link[rel="apple-touch-icon"], link[rel="apple-touch-icon-precomposed"]'));
            var ogImage = document.querySelector('meta[property="og:image"]');
            var themeColorMeta = document.querySelector('meta[name="theme-color"]');

            var manifestUrl = manifestLink ? manifestLink.href : null;
            var themeColor = themeColorMeta ? themeColorMeta.content : null;

            var fallbackIcon = null;
            if (iconLinks.length > 0) {
                var touchIcon = iconLinks.find(function(el) { return (el.rel || '').includes('apple-touch-icon'); });
                if (touchIcon && touchIcon.href) {
                    fallbackIcon = touchIcon.href;
                } else {
                    fallbackIcon = iconLinks[0].href;
                }
            }
            if (!fallbackIcon && ogImage && ogImage.content) {
                fallbackIcon = ogImage.content;
            }
            if (!fallbackIcon) {
                try {
                    fallbackIcon = window.location.origin + '/favicon.ico';
                } catch(e) {}
            }

            var notifyApp = function(manifestJsonStr) {
                if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                    browser.runtime.sendMessage({
                        type: 'PWA_MANIFEST_DISCOVERED',
                        manifestUrl: manifestUrl,
                        manifestJson: manifestJsonStr,
                        documentUrl: window.location.href,
                        pageTitle: document.title,
                        fallbackIcon: fallbackIcon,
                        themeColor: themeColor
                    }).catch(function() {
                        manifestDispatched = false;
                    });
                }
            };

            if (manifestUrl && (manifestUrl.startsWith('http://') || manifestUrl.startsWith('https://'))) {
                fetch(manifestUrl, { credentials: 'same-origin' })
                    .then(function(res) {
                        if (res.ok) return res.text();
                        return null;
                    })
                    .then(function(text) {
                        notifyApp(text);
                    })
                    .catch(function() {
                        if (fallbackIcon) notifyApp(null);
                    });
            } else if (fallbackIcon) {
                notifyApp(null);
            }
        } catch(e) {}
    };

    // =========================================================================
    // 4. In-Realm Article Content Extractor (DOM-First Reader Mode)
    // Extracts clean article elements directly from live DOM without secondary requests
    // =========================================================================
    var articleDispatched = false;
    var scanForArticleContent = function() {
        if (articleDispatched || !document || !document.body) return;

        try {
            var articleRoot = document.querySelector('article, [itemprop="articleBody"], main, .article-content, .post-content, .entry-content, [role="main"]') || document.body;
            if (!articleRoot) return;

            // Extract Title
            var titleEl = document.querySelector('article h1, h1, [itemprop="headline"], meta[property="og:title"]');
            var title = '';
            if (titleEl) {
                title = titleEl.tagName === 'META' ? (titleEl.content || '') : (titleEl.innerText || '');
            }
            if (!title) title = document.title || '';
            title = title.trim();

            // Extract Author/Byline
            var bylineEl = document.querySelector('meta[name="author"], [rel="author"], .byline, [itemprop="author"], .author, .entry-author');
            var byline = '';
            if (bylineEl) {
                byline = bylineEl.tagName === 'META' ? (bylineEl.content || '') : (bylineEl.innerText || '');
            }
            byline = byline.trim();

            // Extract Lead Image
            var ogImage = document.querySelector('meta[property="og:image"]');
            var leadImage = ogImage ? ogImage.content : null;
            if (!leadImage) {
                var firstImg = articleRoot.querySelector('figure img, img');
                if (firstImg && firstImg.src && !firstImg.src.includes('avatar') && !firstImg.src.includes('logo')) {
                    leadImage = firstImg.src;
                }
            }

            // Extract Content Blocks
            var elements = articleRoot.querySelectorAll('p, h2, h3, blockquote');
            if (!elements || elements.length < 2) return;

            var blocks = [];
            var paragraphs = [];
            var totalWords = 0;

            for (var i = 0; i < elements.length; i++) {
                var el = elements[i];
                if (el.offsetParent === null && el.offsetHeight === 0) continue;
                if (el.closest('header, footer, nav, aside, .comments, #comments, [aria-label*="advertisement" i], .ad, .social-share')) continue;

                var text = (el.innerText || '').trim();
                if (text.length < 25 && el.tagName === 'P') continue;
                if (text.includes('Cookie Policy') || text.includes('Privacy Policy') || text.includes('All rights reserved')) continue;

                var blockType = 'PARAGRAPH';
                if (el.tagName === 'H2' || el.tagName === 'H3') {
                    blockType = 'HEADING';
                } else if (el.tagName === 'BLOCKQUOTE') {
                    blockType = 'BLOCKQUOTE';
                }

                blocks.push({ type: blockType, text: text });
                if (blockType === 'PARAGRAPH') {
                    paragraphs.push(text);
                }
                totalWords += text.split(/\s+/).length;
            }

            if (blocks.length === 0) return;
            articleDispatched = true;

            var readingMinutes = Math.max(1, Math.round(totalWords / 200));

            if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendMessage) {
                browser.runtime.sendMessage({
                    type: 'ARTICLE_CONTENT_DISCOVERED',
                    url: window.location.href,
                    title: title,
                    byline: byline,
                    leadImageUrl: leadImage,
                    blocks: blocks,
                    paragraphs: paragraphs,
                    readingMinutes: readingMinutes
                }).catch(function() {
                    articleDispatched = false;
                });
            }
        } catch(e) {}
    };

    if (document.readyState === 'complete') {
        scanForManifest();
        setTimeout(scanForArticleContent, 600);
    } else {
        window.addEventListener('load', function() {
            scanForManifest();
            setTimeout(scanForArticleContent, 600);
        }, { passive: true, once: true });
    }
})();

