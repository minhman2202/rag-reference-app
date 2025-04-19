param location string
param functionAppName string
param appServicePlanName string
param storageAccountName string
param storageBlobEndpoint string
param storageConnectionString string
param docIntelligenceEndpoint string
param docIntelligenceKey string
param tags object

resource appServicePlan 'Microsoft.Web/serverfarms@2022-09-01' = {
  name: appServicePlanName
  location: location
  tags: tags
  sku: {
    name: 'Y1'
    tier: 'Dynamic'
  }
  kind: 'functionapp'
  properties: {
    reserved: true
  }
}

resource functionApp 'Microsoft.Web/sites@2022-09-01' = {
  name: functionAppName
  location: location
  tags: tags
  kind: 'functionapp'
  properties: {
    serverFarmId: appServicePlan.id
    siteConfig: {
      appSettings: [
        {
          name: 'AzureWebJobsStorage'
          value: storageConnectionString
        }
        {
          name: 'FUNCTIONS_EXTENSION_VERSION'
          value: '~4'
        }
        {
          name: 'FUNCTIONS_WORKER_RUNTIME'
          value: 'java'
        }
        {
          name: 'WEBSITE_RUN_FROM_PACKAGE'
          value: '1'
        }
        {
          name: 'AZURE_DOC_INTELLIGENCE_ENDPOINT'
          value: docIntelligenceEndpoint
        }
        {
          name: 'AZURE_DOC_INTELLIGENCE_KEY'
          value: docIntelligenceKey
        }
      ]
      linuxFxVersion: 'JAVA|17'
    }
    httpsOnly: true
  }
  identity: {
    type: 'SystemAssigned'
  }
}

output functionAppId string = functionApp.id
output functionAppUrl string = 'https://${functionApp.properties.defaultHostName}'