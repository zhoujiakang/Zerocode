import { createApp } from 'vue'
import { Button, ConfigProvider, Layout, Menu } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'

const app = createApp(App).use(router)
for (const component of [Button, ConfigProvider, Layout, Menu]) app.use(component)
app.mount('#app')
