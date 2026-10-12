import { request } from './request.js'
export const registerUser = body => request('/user/register', { method: 'POST', body })
export const loginUser = body => request('/user/login', { method: 'POST', body })
export const getLoginUser = () => request('/user/get/login', { silentUnauthorized: true })
export const logoutUser = () => request('/user/logout', { method: 'POST' })
export const searchUsers = body => request('/user/list/page', { method: 'POST', body })
export const deleteUser = id => request('/user/delete', { method: 'POST', body: { id } })
