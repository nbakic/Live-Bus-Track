import { useState, useEffect, useCallback } from "react";

const VISIT_COUNT_KEY = "bus-tracker-visit-count";
const INSTALL_DISMISSED_KEY = "bus-tracker-install-dismissed";
const SHOW_ON_VISITS = [2, 4]; // show on 2nd and 4th visit
const DELAY_MS = 20_000; // 20 seconds

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

function isIOS(): boolean {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) && !(window as any).MSStream;
}

function isStandalone(): boolean {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as any).standalone === true
  );
}

export function useInstallPrompt() {
  const [showBanner, setShowBanner] = useState(false);
  const [isIOSDevice, setIsIOSDevice] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);

  useEffect(() => {
    // Already installed as PWA — never show
    if (isStandalone()) return;

    // User permanently dismissed
    try {
      if (localStorage.getItem(INSTALL_DISMISSED_KEY)) return;
    } catch { /* private browsing */ }

    // Count visits
    let visitCount = 1;
    try {
      visitCount = Number(localStorage.getItem(VISIT_COUNT_KEY) || "0") + 1;
      localStorage.setItem(VISIT_COUNT_KEY, String(visitCount));
    } catch { /* private browsing */ }

    // Only show on target visits
    if (!SHOW_ON_VISITS.includes(visitCount)) return;

    // Capture beforeinstallprompt for Android/Chrome
    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
    };
    window.addEventListener("beforeinstallprompt", handler);

    // Show banner after delay
    const timer = setTimeout(() => {
      setIsIOSDevice(isIOS());
      setShowBanner(true);
    }, DELAY_MS);

    return () => {
      window.removeEventListener("beforeinstallprompt", handler);
      clearTimeout(timer);
    };
  }, []);

  const install = useCallback(async () => {
    if (deferredPrompt) {
      await deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === "accepted") {
        try { localStorage.setItem(INSTALL_DISMISSED_KEY, "1"); } catch {}
      }
      setDeferredPrompt(null);
    }
    setShowBanner(false);
  }, [deferredPrompt]);

  const dismiss = useCallback(() => {
    setShowBanner(false);
  }, []);

  const dismissPermanently = useCallback(() => {
    try { localStorage.setItem(INSTALL_DISMISSED_KEY, "1"); } catch {}
    setShowBanner(false);
  }, []);

  return {
    showBanner,
    isIOSDevice,
    canNativeInstall: !!deferredPrompt,
    install,
    dismiss,
    dismissPermanently,
  };
}
