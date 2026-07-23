Analyze the attached Credit Agreement and extract the information into the following JSON schema.

Every extracted field MUST be represented using:

{
"value":"USD 100,000,000",
"pageNumber":5,
"confidence":1.0,
"sourceText":"US$100,000,000"
}

or 

null for not able to extract fields.

If a value is not explicitly stated:

• Return null for optional objects.

Example:

"loanPurpose": null

• For ExtractedField values return:

{
"value":"",
"pageNumber":0,
"confidence":0.0,
"sourceText":""
}

Do NOT invent values.

Return ONLY valid JSON.

The JSON schema is:

{
"dealName": ExtractedField,
"currency": ExtractedField,
"department": ExtractedField,
"branch": ExtractedField,
"processingAreaCode": ExtractedField,
"classification": ExtractedField,
"agreementDate": ExtractedField,
"globalDealProposedCommitmentAmount": ExtractedField,
"expenseCode": ExtractedField,

"dealAdminAgent": {
"customerExternalId": ExtractedField,
"dealAdminServicingGroup": {
"profileType": ExtractedField
}
},

"dealBorrower": {
"customerExternalId": ExtractedField,
"borrowerIndicator": ExtractedField
},

"interestPricingOptions":[
{
"pricingOption": ExtractedField
}
],

"facilityList":[
{
"dealTrackingNumber": ExtractedField,
"facilityName": ExtractedField,
"facilityType": ExtractedField,
"proposedCommitmentAmount": ExtractedField,
"closingCommitment": ExtractedField,
"agreementDate": ExtractedField,
"effectiveDate": ExtractedField,
"expiryDate": ExtractedField,
"finalMaturityDate": ExtractedField,
"risk":{
  "riskTypeCode": ExtractedField
},

"loanPurpose":{
  "loanPurposeCode": ExtractedField
},

"facilityInterestPricingList":[
  {
      "optionName": ExtractedField,
      "rateBasis": ExtractedField,
      "baseRate": ExtractedField,
      "spread": ExtractedField
  }
],

"facilitySublimit": ExtractedField,

"facilityRid": ExtractedField,

"globalNewAmount": ExtractedField
}
]
}

Return ONLY the JSON object.