import axios from 'axios'

const http = axios.create({
  baseURL: '/agents',
  timeout: 10000
})

export async function fetchAgents() {
  const { data } = await http.get('')
  // data is List<String> — parse each JSON string to object
  return data.map(s => (typeof s === 'string' ? JSON.parse(s) : s))
}

export async function registerRemote(name, body) {
  const { data } = await http.put(`/${encodeURIComponent(name)}`, body)
  return data
}

export async function registerMcp(name, body) {
  const { data } = await http.put(`/mcp/${encodeURIComponent(name)}`, body)
  return data
}

export async function registerRest(name, body) {
  const { data } = await http.put(`/rest/${encodeURIComponent(name)}`, body)
  return data
}

export async function registerRag(name, body) {
  const { data } = await http.put(`/rag/${encodeURIComponent(name)}`, body)
  return data
}

export async function unregisterAgent(name) {
  const { data } = await http.delete(`/${encodeURIComponent(name)}`)
  return data
}
