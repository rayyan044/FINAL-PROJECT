import { useEffect, useState } from "react";

/**
 * Leaflet touches `window` during module initialization. This wrapper keeps every route SSR-safe
 * and loads the actual map only after the browser has hydrated the page.
 */
export function OpenStreetMapLocationPicker(props) {
  const [Picker, setPicker] = useState(null);
  useEffect(() => {
    let mounted = true;
    import("./OpenStreetMapLocationPickerClient").then((module) => {
      if (mounted) setPicker(() => module.OpenStreetMapLocationPickerClient);
    });
    return () => { mounted = false; };
  }, []);
  if (!Picker) return <div aria-live="polite" style={{ height: 300, marginTop: 10, borderRadius: 10, background: "#e5e7eb", display: "grid", placeItems: "center", color: "#475569" }}>Loading map…</div>;
  return <Picker {...props} />;
}
