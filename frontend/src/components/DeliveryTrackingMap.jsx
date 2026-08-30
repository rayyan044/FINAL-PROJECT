import { useEffect, useState } from "react";

export function DeliveryTrackingMap(props) {
  const [Map, setMap] = useState(null);
  useEffect(() => {
    let mounted = true;
    import("./DeliveryTrackingMapClient").then((module) => {
      if (mounted) setMap(() => module.DeliveryTrackingMapClient);
    });
    return () => { mounted = false; };
  }, []);

  if (!Map) return <div style={{ height: 280, borderRadius: 10, background: "#e5e7eb", display: "grid", placeItems: "center" }}>Loading live map…</div>;
  return <Map {...props} />;
}
