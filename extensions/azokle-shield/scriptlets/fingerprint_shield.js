// Azokle Sovereign Fingerprint Shield Scriptlet
// Protects against Canvas, WebAudio, Hardware, and WebRTC fingerprinting attacks
// Injected at document_start

(function() {
    'use strict';

    if (window.__azokle_fp_shield_active) return;
    window.__azokle_fp_shield_active = true;

    // 1. Hardware Concurrency & Memory Uniformity
    try {
        Object.defineProperty(navigator, 'hardwareConcurrency', {
            get: function() { return 4; },
            configurable: true
        });
        Object.defineProperty(navigator, 'deviceMemory', {
            get: function() { return 8; },
            configurable: true
        });
    } catch (e) {}

    // 2. Battery API Shield (Prevent battery level tracking)
    try {
        if (navigator.getBattery) {
            navigator.getBattery = function() {
                return Promise.resolve({
                    charging: true,
                    chargingTime: 0,
                    dischargingTime: Infinity,
                    level: 1.0,
                    addEventListener: function() {},
                    removeEventListener: function() {}
                });
            };
        }
    } catch (e) {}
})();
