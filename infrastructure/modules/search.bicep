param location string
param searchServiceName string
param searchIndexName string
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

resource searchIndex 'Microsoft.Search/searchServices/indexes@2020-08-01' = {
  name: searchIndexName
  parent: searchService
  properties: {
    fields: [
      {
        name: 'id'
        type: 'Edm.String'
        key: true
        searchable: false
        filterable: false
        sortable: false
        facetable: false
        retrievable: true
      }
      {
        name: 'content'
        type: 'Edm.String'
        searchable: true
        filterable: false
        sortable: false
        facetable: false
        retrievable: true
      }
      {
        name: 'metadata'
        type: 'Edm.String'
        searchable: true
        filterable: false
        sortable: false
        facetable: false
        retrievable: true
      }
      {
        name: 'timestamp'
        type: 'Edm.Int64'
        searchable: false
        filterable: true
        sortable: true
        facetable: false
        retrievable: true
      }
    ]
  }
}

output endpoint string = searchService.properties.status == 'running' ? searchService.properties.statusDetails : ''
output serviceName string = searchService.name
output indexName string = searchIndex.name
