param location string = resourceGroup().location
param environment string = 'dev'
param functionAppName string = 'rag-ingestion-function-${environment}'
param storageAccountName string = 'ragstorage${uniqueString(resourceGroup().id)}'
param appServicePlanName string = 'rag-asp-${environment}'
param docIntelligenceName string = 'rag-doc-intelligence-${environment}'

var resourceTags = {
  environment: environment
  project: 'rag-reference-app'
}

module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    location: location
    storageAccountName: storageAccountName
    tags: resourceTags
  }
}

module docIntelligence 'modules/docintelligence.bicep' = {
  name: 'docIntelligence'
  params: {
    location: location
    docIntelligenceName: docIntelligenceName
    tags: resourceTags
  }
}

module function 'modules/function.bicep' = {
  name: 'function'
  params: {
    location: location
    functionAppName: functionAppName
    appServicePlanName: appServicePlanName
    storageAccountName: storageAccountName
    storageBlobEndpoint: storage.outputs.blobEndpoint
    storageConnectionString: storage.outputs.connectionString
    docIntelligenceEndpoint: docIntelligence.outputs.endpoint
    docIntelligenceKey: docIntelligence.outputs.key
    tags: resourceTags
  }
  dependsOn: [
    storage
    docIntelligence
  ]
}

output functionAppUrl string = function.outputs.functionAppUrl
