import { useEffect } from "react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";

const truckPin = L.divIcon({ className: "", html: '<div style="background:#2563eb;border:3px solid white;box-shadow:0 1px 5px #334155;border-radius:50%;width:28px;height:28px;display:grid;place-items:center;font-size:16px">🚚</div>', iconSize: [28, 28], iconAnchor: [14, 14] });

function Recenter({ position }) {
  const map = useMap();
  useEffect(() => { map.setView(position, 15, { animate: true }); }, [map, position]);
  return null;
}

export function DeliveryTrackingMapClient({ latitude, longitude }) {
  const position = [Number(latitude), Number(longitude)];
  return <MapContainer center={position} zoom={15} style={{ height: 280, borderRadius: 10, overflow: "hidden" }}><TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/><Recenter position={position}/><Marker position={position} icon={truckPin}/></MapContainer>;
}
