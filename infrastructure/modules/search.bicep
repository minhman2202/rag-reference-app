param location string
param searchServiceName string
param tags object

resource searchService 'Microsoft.Search/searchServices@2023-11-01' = {
  name: searchServiceName
  location: location
  sku: {
    name: 'basic'
  }
  properties: {
    replicaCount: 1
    partitionCount: 1
    hostingMode: 'default'
  }
  tags: tags
}

output endpoint string = searchService.properties.status == 'running' ? searchService.properties.statusDetails : ''
output serviceName string = searchService.name
