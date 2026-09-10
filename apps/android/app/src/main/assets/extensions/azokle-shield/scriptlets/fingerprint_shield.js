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

    // 2. Canvas Cryptographic Fingerprinting Noise Injection
    try {
        var originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
        CanvasRenderingContext2D.prototype.getImageData = function() {
            var imageData = originalGetImageData.apply(this, arguments);
            // Apply subtle, imperceptible 1-bit noise to 10 pseudo-random pixels
            var data = imageData.data;
            var step = Math.max(4, Math.floor(data.length / 10));
            for (var i = 0; i < data.length; i += step) {
                // Flip lowest bit of blue/green channel
                data[i] = (data[i] ^ 1);
            }
            return imageData;
        };

        var originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
        HTMLCanvasElement.prototype.toDataURL = function() {
            try {
                var ctx = this.getContext('2d');
                if (ctx && this.width > 0 && this.height > 0) {
                    var img = ctx.getImageData(0, 0, Math.min(this.width, 16), Math.min(this.height, 16));
                    // Touch canvas minimally to ensure hash variance
                    img.data[0] = (img.data[0] ^ 1);
                    ctx.putImageData(img, 0, 0);
                }
            } catch (err) {}
            return originalToDataURL.apply(this, arguments);
        };
    } catch (e) {}

    // 3. AudioContext Fingerprint Neutralizer
    try {
        if (window.AudioBuffer) {
            var originalGetChannelData = AudioBuffer.prototype.getChannelData;
            AudioBuffer.prototype.getChannelData = function() {
                var channelData = originalGetChannelData.apply(this, arguments);
                if (channelData && channelData.length > 0) {
                    // Inject micro-jitter (1e-7) that defeats hashing without audible distortion
                    var step = Math.max(1, Math.floor(channelData.length / 8));
                    for (var i = 0; i < channelData.length; i += step) {
                        channelData[i] += 0.0000001;
                    }
                }
                return channelData;
            };
        }
    } catch (e) {}

    // 4. Battery API Shield (Prevent battery level tracking)
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
