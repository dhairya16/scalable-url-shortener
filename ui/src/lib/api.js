import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:80',
  timeout: 10000,
})

export const shortenUrl = ({ longUrl, customCode, ttlDays }) =>
  api.post('/shorten', { longUrl, customCode, ttlDays }).then((res) => res.data)

export const deactivateUrl = (shortCode) => api.delete(`/${shortCode}`)

export const checkUrl = (shortCode) =>
  api.get(`/analytics/${shortCode}`).then((res) => res.data)
