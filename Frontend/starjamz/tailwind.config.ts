import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Electric Lime — primary brand accent (CTAs, waveforms, active states)
        brand: {
          50:  "#F4FFD6",
          100: "#E8FF99",
          200: "#CCFF55",
          300: "#AAFF00", // primary
          400: "#88CC00",
          500: "#66AA00",
          600: "#4D8000",
          700: "#334D00",
          800: "#1A2600",
          900: "#0D1300",
        },
        // Electric Violet — complementary accent (gradients, highlights)
        accent: {
          100: "#DDB3FF",
          200: "#BB66FF",
          300: "#9944FF",
          400: "#7700FF", // primary
          500: "#5500CC",
          600: "#3D0099",
          700: "#280066",
          800: "#140033",
        },
        // Dark green-tinted surfaces (dark-first music app aesthetic)
        surface: {
          base:    "#080C04",
          raised:  "#111A06",
          overlay: "#1A2A0A",
          subtle:  "#222E10",
          muted:   "#2E3D16",
          border:  "#3A4D1C",
          divider: "#2A3A12",
        },
        // Text hierarchy — off-white/cream against dark surfaces
        ink: {
          primary:   "#F0F5E0",
          secondary: "#C8D4A0",
          muted:     "#8A9966",
          disabled:  "#526035",
        },
      },
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "gradient-conic":  "conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))",
        // Brand gradient — hero sections, featured cards
        "brand-gradient": "linear-gradient(135deg, #AAFF00 0%, #7700FF 100%)",
        // Subtle radial lime glow for hero backgrounds
        "lime-glow": "radial-gradient(ellipse 60% 40% at 50% 0%, rgba(170,255,0,0.18) 0%, transparent 70%)",
        // Vertical lime for waveform / audio visualiser bars
        "waveform-gradient": "linear-gradient(to top, #AAFF00, rgba(170,255,0,0.3))",
      },
      boxShadow: {
        "lime-sm": "0 0 8px rgba(170,255,0,0.35)",
        "lime-md": "0 0 20px rgba(170,255,0,0.45)",
        "lime-lg": "0 0 40px rgba(170,255,0,0.35), 0 0 80px rgba(170,255,0,0.15)",
        "violet-sm": "0 0 8px rgba(119,0,255,0.45)",
        "violet-md": "0 0 20px rgba(119,0,255,0.55)",
      },
      animation: {
        "pulse-lime": "pulse-lime 2s ease-in-out infinite",
        "waveform":   "waveform 1.2s ease-in-out infinite alternate",
        "slide-up":   "slide-up 0.3s ease-out",
        "fade-in":    "fade-in 0.25s ease-out",
      },
      keyframes: {
        "pulse-lime": {
          "0%, 100%": { boxShadow: "0 0 8px rgba(170,255,0,0.35)" },
          "50%":       { boxShadow: "0 0 24px rgba(170,255,0,0.7)" },
        },
        "waveform": {
          "0%":   { transform: "scaleY(0.3)" },
          "100%": { transform: "scaleY(1)" },
        },
        "slide-up": {
          "0%":   { transform: "translateY(12px)", opacity: "0" },
          "100%": { transform: "translateY(0)",    opacity: "1" },
        },
        "fade-in": {
          "0%":   { opacity: "0" },
          "100%": { opacity: "1" },
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "Fira Code", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
