param location string
param searchServiceName string
param tags object

resource searchService 'Microsoft.Search/searchServices@2023-11-01' = {
  name: searchServiceName
  location: location
  sku: {
    name: 'free'
  }
  properties: {
    replicaCount: 1
    partitionCount: 1
    hostingMode: 'default'
  }
  tags: tags
}

resource searchIndex 'Microsoft.Search/searchServices/indexes@2023-11-01' = {
  parent: searchService
  name: 'documents'
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
        name: 'fileName'
        type: 'Edm.String'
        searchable: true
        filterable: true
        sortable: true
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
        analyzer: 'standard.lucene'
      }
      {
        name: 'metadata'
        type: 'Edm.String'
        searchable: true
        filterable: true
        sortable: false
        facetable: false
        retrievable: true
      }
      {
        name: 'uploadDate'
        type: 'Edm.Int64'
        searchable: false
        filterable: true
        sortable: true
        facetable: true
        retrievable: true
      }
    ]
  }
}

output searchServiceEndpoint string = 'https://${searchService.name}.search.windows.net'
output searchServiceKey string = listAdminKeys(searchService.id, '2023-11-01').primaryKey
