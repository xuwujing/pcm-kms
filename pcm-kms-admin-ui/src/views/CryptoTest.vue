<template>
  <div>
    <el-row :gutter="20">
      <!-- 加密 -->
      <el-col :span="12">
        <el-card>
          <template #header>加密</template>
          <el-form label-width="80px">
            <el-form-item label="密钥别名">
              <el-input v-model="encryptForm.alias" placeholder="如 kms_xxx_default" />
            </el-form-item>
            <el-form-item label="应用组">
              <el-input v-model="encryptForm.clientGroup" placeholder="default" />
            </el-form-item>
            <el-form-item label="明文">
              <el-input v-model="encryptForm.plainText" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleEncrypt">加密</el-button>
            </el-form-item>
            <el-form-item v-if="encryptResult" label="密文">
              <el-input :model-value="encryptResult.cipherText" type="textarea" :rows="3" readonly />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 解密 -->
      <el-col :span="12">
        <el-card>
          <template #header>解密</template>
          <el-form label-width="80px">
            <el-form-item label="密钥别名">
              <el-input v-model="decryptForm.alias" placeholder="如 kms_xxx_default" />
            </el-form-item>
            <el-form-item label="应用组">
              <el-input v-model="decryptForm.clientGroup" placeholder="default" />
            </el-form-item>
            <el-form-item label="密文">
              <el-input v-model="decryptForm.cipherText" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="handleDecrypt">解密</el-button>
            </el-form-item>
            <el-form-item v-if="decryptResult" label="明文">
              <el-input :model-value="decryptResult.plainText" type="textarea" :rows="3" readonly />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 摘要 -->
    <el-card style="margin-top: 20px;">
      <template #header>摘要</template>
      <el-form label-width="80px" inline>
        <el-form-item label="算法">
          <el-select v-model="digestForm.algorithm">
            <el-option label="MD5" value="md5" />
            <el-option label="SM3" value="sm3" />
          </el-select>
        </el-form-item>
        <el-form-item label="原文">
          <el-input v-model="digestForm.plainText" style="width: 300px" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="handleDigest">计算摘要</el-button>
        </el-form-item>
        <el-form-item v-if="digestResult" label="结果">
          <el-input :model-value="digestResult.cipherText" readonly style="width: 400px" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { encrypt, decrypt, digest } from '../api'

const encryptForm = ref({ alias: '', clientGroup: 'default', plainText: '' })
const decryptForm = ref({ alias: '', clientGroup: 'default', cipherText: '' })
const digestForm = ref({ algorithm: 'md5', plainText: '' })

const encryptResult = ref(null)
const decryptResult = ref(null)
const digestResult = ref(null)

const handleEncrypt = async () => {
  if (!encryptForm.value.alias || !encryptForm.value.plainText) {
    ElMessage.warning('请填写别名和明文')
    return
  }
  const res = await encrypt(encryptForm.value)
  encryptResult.value = res.data
}

const handleDecrypt = async () => {
  if (!decryptForm.value.alias || !decryptForm.value.cipherText) {
    ElMessage.warning('请填写别名和密文')
    return
  }
  const res = await decrypt(decryptForm.value)
  decryptResult.value = res.data
}

const handleDigest = async () => {
  if (!digestForm.value.plainText) {
    ElMessage.warning('请输入原文')
    return
  }
  const res = await digest(digestForm.value)
  digestResult.value = res.data
}
</script>
