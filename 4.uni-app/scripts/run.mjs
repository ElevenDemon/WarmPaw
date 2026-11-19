import { spawn } from 'node:child_process'
import { readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
const root = fileURLToPath(new URL('../', import.meta.url))
const args = process.argv.slice(2)
const child = spawn(process.execPath, ['node_modules/@dcloudio/vite-plugin-uni/bin/uni.js', ...args], {cwd:root, stdio:'inherit', env:{...process.env, UNI_INPUT_DIR:root}, windowsHide:true})
child.on('exit', code => {
  if (code === 0 && args[0] === 'build' && args.includes('mp-weixin')) {
    try {
      const configPath = join(root, 'dist/build/mp-weixin/project.config.json')
      const config = JSON.parse(readFileSync(configPath, 'utf8'))
      delete config.miniprogramRoot
      writeFileSync(configPath, `${JSON.stringify(config, null, 2)}\n`)
    } catch (error) {
      console.error('Failed to prepare WeChat project configuration:', error)
      process.exit(1)
    }
  }
  process.exit(code ?? 1)
})
