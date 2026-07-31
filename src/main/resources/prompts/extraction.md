Analyze the attached Credit Agreement.

The document is provided as a JSON list of pages. Each element has:
- "page": the page number (integer)
- "content": the full text of that page

Example input:

[
  { "pageNumber": 1, "text": "John Doe..." },
  { "pageNumber": 2, "text": "Policy Number ABC123..." },
  { "pageNumber": 3, "text": "Address Bangalore..." }
]

For EVERY extracted field, follow this procedure exactly:

1. Iterate through each page in the list, in order.
2. Find the page whose "content" contains the supporting text for the field.
3. Record that page's "page" number.
4. Extract the value from that supporting text.
5. Set pageNumber to the recorded "page" number, exactly as provided in the input.

Rules:

- Never guess or invent the page number.
- Return the page number EXACTLY as given in the input list.
- If the value appears on multiple pages, return the FIRST occurrence.
- The pageNumber and sourceText MUST always correspond to the same page.

Return ONLY valid JSON.

Every extracted leaf field must use:

{
"value":"",
"pageNumber":0,
"confidence":0.0,
"sourceText":""
}

When a value cannot be found:

Return null.

Do NOT invent values.

Do NOT infer values.

Do NOT estimate values.

Return every schema field.

Never omit fields.

Nested objects must always exist.

Arrays must always be arrays.

Extract the information into the following JSON schema.

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

• Nested objects (dealAdminAgent, dealAdminServicingGroup, risk, loanPurpose)
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

"dealBorrower": ExtractedField,
"borrowerIndicator": ExtractedField,

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