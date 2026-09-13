import SwiftUI

// MARK: - Ember Particle Model
private struct PureEmberParticle {
    var xPercent: CGFloat
    var yPercent: CGFloat
    let radius: CGFloat
    let ascentSpeed: CGFloat
    let horizontalWander: CGFloat
    let swayPhase: CGFloat
    let color: Color
    let baseAlpha: Double
}

// MARK: - Sovereign Fire Incineration Overlay
public struct FireIncinerationOverlay: View {
    let onComplete: () -> Void

    @State private var veilAlpha: Double = 0.0
    @State private var contentScale: CGFloat = 0.88
    @State private var ring1Scale: CGFloat = 0.8
    @State private var ring1Alpha: Double = 0.0
    @State private var ring2Scale: CGFloat = 0.6
    @State private var ring2Alpha: Double = 0.0
    @State private var time: Double = 0.0

    @State private var embers: [PureEmberParticle] = {
        let colors = [
            Color(hex: 0xFF9800), // Amber
            Color(hex: 0xFFFFB300), // Golden Amber
            Color(hex: 0xFFF97316), // Warm Orange
            Color(hex: 0xFFFFA000)  // Gold
        ]
        return (0..<24).map { i in
            PureEmberParticle(
                xPercent: CGFloat.random(in: 0.1...0.9),
                yPercent: CGFloat.random(in: 0.5...1.0),
                radius: CGFloat.random(in: 1.5...3.2),
                ascentSpeed: CGFloat.random(in: 0.12...0.28),
                horizontalWander: CGFloat.random(in: 0.01...0.03),
                swayPhase: CGFloat.random(in: 0...6.28),
                color: colors[i % colors.count],
                baseAlpha: Double.random(in: 0.6...0.95)
            )
        }
    }()

    let timer = Timer.publish(every: 0.016, on: .main, in: .common).autoconnect()

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            // 1. Particle Canvas: Shockwave Rings & Rising Embers
            Canvas { context, size in
                let w = size.width
                let h = size.height
                let centerPoint = CGPoint(x: w / 2, y: h * 0.44)

                // Expanding Ring 1
                if ring1Alpha > 0.01 {
                    let r1 = 44 * ring1Scale
                    let path1 = Path(ellipseIn: CGRect(x: centerPoint.x - r1, y: centerPoint.y - r1, width: r1 * 2, height: r1 * 2))
                    context.stroke(path1, with: .color(Color(hex: 0xFFFF9800).opacity(ring1Alpha * 0.6)), lineWidth: 1.5)
                }

                // Expanding Ring 2
                if ring2Alpha > 0.01 {
                    let r2 = 44 * ring2Scale
                    let path2 = Path(ellipseIn: CGRect(x: centerPoint.x - r2, y: centerPoint.y - r2, width: r2 * 2, height: r2 * 2))
                    context.stroke(path2, with: .color(Color(hex: 0xFFF97316).opacity(ring2Alpha * 0.4)), lineWidth: 1.0)
                }

                // Rising Ember Particles
                for ember in embers {
                    let sway = sin(Double(ember.yPercent * 8 + ember.swayPhase + CGFloat(time)))
                    let drawX = (ember.xPercent + CGFloat(sway) * ember.horizontalWander) * w
                    let drawY = ember.yPercent * h

                    if drawY > -20 && drawY < h + 20 {
                        let progress = max(0, min(1, 1 - (drawY / h)))
                        let fadeAlpha = max(0, min(1, ember.baseAlpha * (1 - progress * 0.65) * veilAlpha))
                        let rect = CGRect(x: drawX - ember.radius, y: drawY - ember.radius, width: ember.radius * 2, height: ember.radius * 2)
                        let circle = Path(ellipseIn: rect)
                        context.fill(circle, with: .color(ember.color.opacity(fadeAlpha)))
                    }
                }
            }
            .ignoresSafeArea()

            // 2. Minimalist Centerpiece Disc & Status Pill
            VStack(spacing: 20) {
                // Flame Disc
                ZStack {
                    Circle()
                        .fill(Color(hex: 0x141416))
                        .frame(width: 76, height: 76)
                        .overlay(Circle().stroke(Color(hex: 0xFFFF9800), lineWidth: 1.5))
                        .shadow(color: Color(hex: 0xFFFF9800).opacity(0.4), radius: 16)

                    Image(systemName: "flame.fill")
                        .font(.system(size: 38))
                        .foregroundColor(Color(hex: 0xFFFF9800))
                }

                // Status Pill
                VStack(spacing: 4) {
                    HStack(spacing: 6) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 13))
                            .foregroundColor(Color(hex: 0xFFFF9800))

                        Text("SESSION INCINERATED")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                            .tracking(1.0)
                    }

                    Text("All tabs, cookies, cache, and history destroyed")
                        .font(.system(size: 11))
                        .foregroundColor(Color(hex: 0x9E9EA8))
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color(hex: 0x141416))
                .cornerRadius(20)
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.2), lineWidth: 1))
            }
            .scaleEffect(contentScale)
        }
        .opacity(veilAlpha)
        .onReceive(timer) { _ in
            time += 0.05
            for i in 0..<embers.count {
                embers[i].yPercent -= (embers[i].ascentSpeed * 0.015)
                if embers[i].yPercent < 0 {
                    embers[i].yPercent = 1.05
                }
            }
        }
        .onAppear {
            // Tactile Haptic Vibration
            let generator = UINotificationFeedbackGenerator()
            generator.prepare()
            generator.notificationOccurred(.warning)

            let impact = UIImpactFeedbackGenerator(style: .heavy)
            impact.prepare()
            impact.impactOccurred()

            // Smooth reveal animation
            withAnimation(.easeOut(duration: 0.3)) {
                veilAlpha = 1.0
                contentScale = 1.0
                ring1Alpha = 0.6
                ring1Scale = 2.2
                ring2Alpha = 0.4
                ring2Scale = 3.0
            }

            // Exit fade and callback
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                withAnimation(.easeIn(duration: 0.3)) {
                    self.veilAlpha = 0.0
                }
            }

            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                self.onComplete()
            }
        }
    }
}
