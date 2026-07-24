Analyze the attached Credit Agreement and extract the information into the following JSON schema.

Every extracted (leaf) field MUST be represented using the ExtractedField structure:

{
"value":"USD 100,000,000",
"pageNumber":5,
"confidence":1.0,
"sourceText":"US$100,000,000"
}

STRICT NULL & STRUCTURE RULES:

• Every field in the schema MUST always be present. Never omit a field.

• A leaf field is either the ExtractedField object (when a value is found) or a bare null
  (when the value cannot be found). When not found, set the leaf directly to null:

"dealName": null

A leaf field must NEVER be {}, "", or [].

• Nested objects (dealAdminAgent, dealAdminServicingGroup, dealBorrower, risk, loanPurpose)
  MUST always be present as objects with their inner fields. When nothing is found, still return
  the object with its inner leaves set to null.

Correct:

"loanPurpose":{
"loanPurposeCode": null
}

WRONG: "loanPurpose": null
WRONG: "loanPurpose": []

A nested object must NEVER be null and NEVER be [].

• Arrays (interestPricingOptions, facilityList, facilityInterestPricingList) MUST always be arrays.
  Return an empty array [] only when no such items exist. An array must NEVER be null.

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