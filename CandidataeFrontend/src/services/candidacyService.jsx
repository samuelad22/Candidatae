import { fetchJson } from "../utils/fetch";

export const getElementos = () => fetchJson("/candidaturas")