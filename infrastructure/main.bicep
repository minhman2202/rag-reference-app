param location string = resourceGroup().location
param environment string = 'dev'
param functionAppName string = 'rag-function-${environment}'
param storageAccountName string = 'ragstorageinqs4uargpjkc'
param appServicePlanName string = 'rag-asp-${environment}'
param docIntelligenceName string = 'rag-doc-intelligence-${environment}'
param searchServiceName string = 'rag-search-${environment}'

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

module search 'modules/search.bicep' = {
  name: 'search'
  params: {
    location: location
    searchServiceName: searchServiceName
    tags: resourceTags
  }
}

module function 'modules/function.bicep' = {
  name: 'function'
  params: {
    location: location
    functionAppName: functionAppName
    appServicePlanName: appServicePlanName
    storageConnectionString: storage.outputs.connectionString
    docIntelligenceEndpoint: docIntelligence.outputs.endpoint
    docIntelligenceKey: docIntelligence.outputs.key
    searchEndpoint: search.outputs.endpoint
    searchServiceName: search.outputs.serviceName
    tags: resourceTags
  }
}

output functionAppUrl string = function.outputs.functionAppUrl
output searchServiceEndpoint string = search.outputs.endpoint
