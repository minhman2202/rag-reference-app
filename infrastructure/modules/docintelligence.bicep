param location string
param docIntelligenceName string
param tags object

resource docIntelligence 'Microsoft.CognitiveServices/accounts@2023-05-01' = {
  name: docIntelligenceName
  location: location
  tags: tags
  sku: {
    name: 'F0'
  }
  kind: 'FormRecognizer'
  properties: {
    customSubDomainName: docIntelligenceName
  }
}

output endpoint string = docIntelligence.properties.endpoint
output key string = docIntelligence.listKeys().key1 