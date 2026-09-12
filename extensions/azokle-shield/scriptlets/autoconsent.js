// Azokle AutoConsent Sovereign Scriptlet
// Automatically detects Consent Management Platforms (CMPs) and rejects tracking cookies
// Suppresses annoying cookie banners (OneTrust, Didomi, Cookiebot, TrustArc, Quantcast Choice)

(function() {
    'use strict';

    if (window.__azokle_autoconsent_initialized) return;
    window.__azokle_autoconsent_initialized = true;

    var MAX_ATTEMPTS = 12;
    var attempts = 0;

    function findAndClickRejectButton() {
        // 1. OneTrust CMP
        var oneTrustReject = document.querySelector('#onetrust-reject-all-handler, .ot-pc-refuse-all-handler, button#accept-recommended-btn-handler');
        if (oneTrustReject && isVisible(oneTrustReject)) {
            oneTrustReject.click();
            hideElement('#onetrust-consent-sdk');
            unlockScroll();
            return true;
        }

        // 2. Cookiebot CMP
        var cookiebotReject = document.querySelector('#CybotCookiebotDialogBodyLevelButtonLevelOptinDeclineAll, #CybotCookiebotDialogBodyButtonDecline');
        if (cookiebotReject && isVisible(cookiebotReject)) {
            cookiebotReject.click();
            hideElement('#CybotCookiebotDialog');
            unlockScroll();
            return true;
        }

        // 3. Didomi CMP
        var didomiDisagree = document.querySelector('#didomi-notice-disagree-button, .didomi-continue-without-agreeing, button[id*="didomi-notice-disagree"]');
        if (didomiDisagree && isVisible(didomiDisagree)) {
            didomiDisagree.click();
            hideElement('#didomi-host');
            unlockScroll();
            return true;
        }

        // 4. Quantcast Choice CMP
        var qcReject = document.querySelector('.qc-cmp2-summary-buttons button[mode="secondary"], button.qc-cmp2-button[mode="secondary"]');
        if (qcReject && isVisible(qcReject)) {
            qcReject.click();
            hideElement('.qc-cmp2-container');
            unlockScroll();
            return true;
        }

        // 5. TrustArc CMP
        var trustArcReject = document.querySelector('a.call, .trustarc-banner button.reject, #truste-consent-required');
        if (trustArcReject && isVisible(trustArcReject)) {
            trustArcReject.click();
            hideElement('.trustarc-banner-container, #truste-consent-track');
            unlockScroll();
            return true;
        }

        // 6. Generic Heuristic Button Matcher
        var buttons = document.querySelectorAll('button, a[role="button"], input[type="button"]');
        var rejectRegex = /^(reject all|decline all|only necessary|essential only|reject non-essential|refuse all|deny all|reject|decline)$/i;

        for (var i = 0; i < buttons.length; i++) {
            var btn = buttons[i];
            var text = (btn.innerText || btn.textContent || btn.value || '').trim();
            if (rejectRegex.test(text) && isVisible(btn)) {
                var parentDialog = btn.closest('[role="dialog"], [role="alertdialog"], [id*="cookie"], [class*="cookie"], [id*="consent"], [class*="consent"]');
                if (parentDialog) {
                    btn.click();
                    parentDialog.style.setProperty('display', 'none', 'important');
                    unlockScroll();
                    return true;
                }
            }
        }

        return false;
    }

    function isVisible(el) {
        if (!el) return false;
        var style = window.getComputedStyle(el);
        return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0' && el.offsetWidth > 0;
    }

    function hideElement(selector) {
        try {
            var el = document.querySelector(selector);
            if (el) {
                el.style.setProperty('display', 'none', 'important');
                el.style.setProperty('visibility', 'hidden', 'important');
            }
        } catch(e) {}
    }

    function unlockScroll() {
        try {
            document.documentElement.style.removeProperty('overflow');
            document.body.style.removeProperty('overflow');
            document.documentElement.classList.remove('modal-open', 'ot-modal-open', 'didomi-popup-open');
            document.body.classList.remove('modal-open', 'ot-modal-open', 'didomi-popup-open');
        } catch(e) {}
    }

    // Polling loop to catch delayed CMP injections without impacting battery
    var interval = setInterval(function() {
        attempts++;
        var handled = findAndClickRejectButton();
        if (handled || attempts >= MAX_ATTEMPTS) {
            clearInterval(interval);
        }
    }, 400);

    // Also listen for DOM mutation
    if (document.body) {
        var observer = new MutationObserver(function(mutations, obs) {
            if (findAndClickRejectButton()) {
                obs.disconnect();
                clearInterval(interval);
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
        setTimeout(function() { observer.disconnect(); }, 8000);
    }
})();
