import axios from 'axios'

const api = axios.create({
  baseURL: '/',
  timeout: 10000,
})

export const shortenUrl = ({ longUrl, customCode, ttlDays }) =>
  api.post('/shorten', { longUrl, customCode, ttlDays }).then((res) => res.data)

export const deactivateUrl = (shortCode) =>
  api.delete(`/${shortCode}`, {
    headers: { 'Content-Type': 'application/json' },
  })
