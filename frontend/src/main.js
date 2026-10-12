import { createApp } from 'vue'
import { Avatar, Button, ConfigProvider, Dropdown, Layout, Menu } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'

const app = createApp(App).use(pinia).use(router)
for (const component of [Avatar, Button, ConfigProvider, Dropdown, Layout, Menu]) app.use(component)
app.mount('#app')
