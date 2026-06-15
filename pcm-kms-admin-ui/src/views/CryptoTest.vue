<template>
  <div class="crypto-page">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>加密</template>
          <el-form label-width="100px">
            <el-form-item label="别名">
              <el-input v-model="encryptForm.alias" placeholder="user-phone-aes" />
            </el-form-item>
            <el-form-item label="应用分组">
              <el-input v-model="encryptForm.clientGroup" placeholder="default" />
            </el-form-item>
            <el-form-item label="明文">
              <el-input v-model="encryptForm.plainText" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleEncrypt">执行加密</el-button>
            </el-form-item>
            <el-form-item label="密文">
              <el-input :model-value="encryptResult?.cipherText || ''" type="textarea" :rows="4" readonly />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>解密</template>
          <el-form label-width="100px">
            <el-form-item label="别名">
              <el-input v-model="decryptForm.alias" placeholder="user-phone-aes" />
            </el-form-item>
            <el-form-item label="应用分组">
              <el-input v-model="decryptForm.clientGroup" placeholder="default" />
            </el-form-item>
            <el-form-item label="密文">
              <el-input v-model="decryptForm.cipherText" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="handleDecrypt">执行解密</el-button>
            </el-form-item>
            <el-form-item label="明文">
              <el-input :model-value="decryptResult?.plainText || ''" type="textarea" :rows="4" readonly />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="digest-card">
      <template #header>摘要</template>
      <el-form inline label-width="100px">
        <el-form-item label="算法">
          <el-select v-model="digestForm.algorithm" style="width: 140px">
            <el-option label="MD5" value="md5" />
            <el-option label="SM3" value="sm3" />
          </el-select>
        </el-form-item>
        <el-form-item label="明文">
          <el-input v-model="digestForm.plainText" style="width: 320px" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="handleDigest">计算摘要</el-button>
        </el-form-item>
        <el-form-item label="摘要结果">
          <el-input :model-value="digestResult?.cipherText || ''" style="width: 420px" readonly />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { decrypt, digest, encrypt } from '../api'

const encryptForm = ref({
  alias: '',
  clientGroup: 'default',
  plainText: '',
})
const decryptForm = ref({
  alias: '',
  clientGroup: 'default',
  cipherText: '',
})
const digestForm = ref({
  algorithm: 'md5',
  plainText: '',
})

const encryptResult = ref(null)
const decryptResult = ref(null)
const digestResult = ref(null)

const handleEncrypt = async () => {
  if (!encryptForm.value.alias || !encryptForm.value.plainText) {
    ElMessage.warning('请填写别名和明文')
    return
  }
  const res = await encrypt(encryptForm.value)
  encryptResult.value = res.data || res
}

const handleDecrypt = async () => {
  if (!decryptForm.value.alias || !decryptForm.value.cipherText) {
    ElMessage.warning('请填写别名和密文')
    return
  }
  const res = await decrypt(decryptForm.value)
  decryptResult.value = res.data || res
}

const handleDigest = async () => {
  if (!digestForm.value.plainText) {
    ElMessage.warning('请填写明文')
    return
  }
  const res = await digest(digestForm.value)
  digestResult.value = res.data || res
}
</script>

<style scoped>
.crypto-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.digest-card {
  margin-top: 4px;
}
</style>
