import { api } from "./api";
export const listTransportDistanceRates=()=>api.get("/transport-distance-rates").then(r=>r.data);
export const saveTransportDistanceRate=(payload,id)=> (id?api.put(`/transport-distance-rates/${id}`,payload):api.post("/transport-distance-rates",payload)).then(r=>r.data);
