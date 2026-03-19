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
        // ── Primary brand: Electric Lime ──────────────────────────────
        // Anchor: #AAFF00. Scale built by shifting lightness in HSL(82°,100%)
        brand: {
          50:  "#F4FFD6",
          100: "#E6FFAA",
          200: "#CCFF55",
          300: "#AAFF00", // ← hero / primary
          400: "#88CC00",
          500: "#669900",
          600: "#4D7300",
          700: "#334D00",
          800: "#1A2600",
          900: "#0D1300",
          950: "#060800",
        },

        // ── Complementary accent: Electric Violet ─────────────────────
        // Hue 270° opposite lime on the wheel — used for gradients & highlights
        accent: {
          50:  "#F2E6FF",
          100: "#D9AAFF",
          200: "#BB77FF",
          300: "#9944FF",
          400: "#7700FF", // ← complementary hero
          500: "#5500CC",
          600: "#440099",
          700: "#330066",
          800: "#1A0033",
          900: "#0D001A",
          950: "#060010",
        },

        // ── Surfaces (dark-first, slight green tint) ──────────────────
        surface: {
          base:    "#080C04", // page background
          raised:  "#111A06", // cards, sidebars
          overlay: "#1A2A0A", // modals, popovers
          subtle:  "#222E10", // secondary bg
          muted:   "#2E3D16", // tertiary / hover bg
          border:  "#3A4D1C", // default borders
          divider: "#2A3A12", // hairline dividers
        },

        // ── Ink (text & icon) ─────────────────────────────────────────
        ink: {
          primary:   "#F0F5E0", // headings / high-emphasis
          secondary: "#C8D4A0", // body / medium-emphasis
          muted:     "#8A9966", // captions / low-emphasis
          disabled:  "#526035", // disabled state
          inverse:   "#0A0D05", // text on light / brand backgrounds
        },

        // ── Semantic ──────────────────────────────────────────────────
        positive: {
          DEFAULT: "#AAFF00",
          subtle:  "#1A2600",
        },
        warning: {
          DEFAULT: "#FFD000",
          subtle:  "#2A2000",
        },
        danger: {
          DEFAULT: "#FF4455",
          subtle:  "#2A0A0E",
        },
        info: {
          DEFAULT: "#00CCFF",
          subtle:  "#001A22",
        },
      },

      backgroundImage: {
        // Signature gradient — lime → violet
        "brand-gradient":   "linear-gradient(135deg, #AAFF00 0%, #7700FF 100%)",
        // Subtle radial glow for hero sections
        "lime-glow":        "radial-gradient(ellipse at 50% 0%, rgba(170,255,0,0.18) 0%, transparent 70%)",
        // Waveform / visualiser gradient
        "waveform-gradient":"linear-gradient(180deg, #AAFF00 0%, #669900 100%)",
        // Card shimmer
        "card-shimmer":     "linear-gradient(90deg, transparent 0%, rgba(170,255,0,0.06) 50%, transparent 100%)",
        "gradient-radial":  "radial-gradient(var(--tw-gradient-stops))",
        "gradient-conic":   "conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))",
      },

      boxShadow: {
        // Glow effects keyed to the brand color
        "lime-sm":  "0 0 8px 0 rgba(170,255,0,0.35)",
        "lime-md":  "0 0 20px 0 rgba(170,255,0,0.30)",
        "lime-lg":  "0 0 40px 0 rgba(170,255,0,0.25)",
        "violet-sm":"0 0 8px 0 rgba(119,0,255,0.40)",
        "violet-md":"0 0 20px 0 rgba(119,0,255,0.35)",
      },

      ringColor: {
        brand: "#AAFF00",
        accent: "#7700FF",
      },
    },
  },
  plugins: [],
};

export default config;
