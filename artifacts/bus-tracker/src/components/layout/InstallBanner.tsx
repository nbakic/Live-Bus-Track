import { X, Share, Plus } from "lucide-react";
import { useInstallPrompt } from "@/hooks/use-install-prompt";
import { cn } from "@/lib/utils";

export function InstallBanner() {
  const { showBanner, isIOSDevice, canNativeInstall, install, dismiss, dismissPermanently } =
    useInstallPrompt();

  if (!showBanner) return null;

  return (
    <div className="fixed bottom-20 left-3 right-3 z-[1500] flex justify-center pointer-events-none animate-fade-in">
      <div
        className={cn(
          "pointer-events-auto w-full max-w-sm rounded-xl overflow-hidden",
          "bg-white/90 dark:bg-gray-900/90 backdrop-blur-xl",
          "border border-black/[0.06] dark:border-white/[0.08]",
          "shadow-lg"
        )}
      >
        <div className="px-4 pt-3 pb-3">
          {/* Header */}
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-center gap-2.5">
              <img src="/favicon.svg" alt="" className="w-9 h-9 rounded-lg" />
              <div>
                <p className="text-sm font-semibold text-gray-800 dark:text-gray-100">
                  Bus Tracker
                </p>
                <p className="text-[11px] text-gray-500 dark:text-gray-400">
                  Dodaj na početni ekran
                </p>
              </div>
            </div>
            <button
              onClick={dismiss}
              className="shrink-0 w-6 h-6 -mr-1 -mt-0.5 rounded-md flex items-center justify-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              aria-label="Zatvori"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* iOS instructions */}
          {isIOSDevice && !canNativeInstall && (
            <div className="mt-2.5 flex items-center gap-2 text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
              <span>Klikni</span>
              <Share className="w-3.5 h-3.5 shrink-0 text-blue-500" />
              <span>pa</span>
              <span className="inline-flex items-center gap-0.5 font-medium text-gray-700 dark:text-gray-200">
                <Plus className="w-3 h-3" /> Add to Home Screen
              </span>
            </div>
          )}

          {/* Action buttons */}
          <div className="mt-3 flex items-center gap-2">
            {canNativeInstall ? (
              <button
                onClick={install}
                className={cn(
                  "flex-1 py-2 rounded-lg text-xs font-medium",
                  "bg-[#FF3C00] text-white",
                  "hover:bg-[#e63600] active:scale-[0.98]",
                  "transition-all"
                )}
              >
                Instaliraj
              </button>
            ) : !isIOSDevice ? (
              <p className="flex-1 text-[11px] text-gray-500 dark:text-gray-400">
                Brži pristup s početnog ekrana
              </p>
            ) : null}
            <button
              onClick={dismissPermanently}
              className="py-2 px-3 rounded-lg text-[11px] text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
            >
              Ne prikazuj
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
