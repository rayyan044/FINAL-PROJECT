import DeliveryCard from "../deliveries/DeliveryCard";

export default function DashboardDeliveryCard({ delivery, onOpen }) {
  return <DeliveryCard delivery={delivery} onViewDetails={onOpen} />;
}
