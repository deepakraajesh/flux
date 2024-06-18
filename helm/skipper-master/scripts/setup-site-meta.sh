#!/bin/sh
#these site meta data are necessary to create site

if [ -z $skipperHost ];
then 
  skipperHost="skipper.ss"
fi

echo "skipper host is ${skipperHost}"

echo "set up data center data with request ${datacenter}"
curl -X POST "http://${skipperHost}/admin/site/datacenter" -H "Content-Type: application/json" -d "${datacenter}"

echo "\nset up environments data"
curl -X POST "http://${skipperHost}/admin/site/meta/environments" -H "Content-Type: application/json" -d '[{"id":"Development","name":"Dev"},{"id":"Production","name":"Prod"},{"id":"Staging","name":"Staging"},{"id":"QA","name":"QA"}]'

echo "\nset up platforms data"
curl -X POST "http://${skipperHost}/admin/site/meta/platforms" -H "Content-Type: application/json" -d '[{"id":"Magento","name":"Magento"},{"id":"Shopify","name":"Shopify"},{"id":"Wordpress","name":"Wordpress"},{"id":"WooCommerceWIX","name":"WooCommerceWIX"},{"id":"Sitebuilder.com","name":"Sitebuilder.com"},{"id":"BigCommerce","name":"BigCommerce"},{"id":"Volusion","name":"Volusion"},{"id":"Opencart","name":"Opencart"},{"id":"Prestashop","name":"Prestashop"},{"id":"ATG(Oracle)","name":"ATG(Oracle)"},{"id":"IBMwebsphere","name":"IBMwebsphere"},{"id":"Hybris","name":"Hybris"},{"id":"SalesforceCommerce","name":"SalesforceCommerce"}]'

echo "\nset up verticals data"
curl -X POST "http://${skipperHost}/admin/site/meta/verticals" -H "Content-Type: application/json" -d '[{"id":"apparel","name":"Fashion & Apparel"},{"id":"electronics","name":"Electronics"},{"id":"hardware","name":"Hardware & B2B"},{"id":"home","name":"Home & Living"},{"id":"health","name":"Healthcare"},{"id":"beauty","name":"Beauty & Cosmetics"},{"id":"jewelry","name":"Jewellery & Accessories"},{"id":"autoparts","name":"Autoparts"},{"id":"mass_merchant","name":"Mass merchant"},{"id":"food","name":"Food & Grocery"},{"id":"pets","name":"Pet Products"},{"id":"other","name":"Other"},{"id":"liquor","name":"Beverages"}]'

echo "\nset up languages data"
curl -X POST 'http://skipper.ss/admin/site/meta/languages' -H "Content-Type: application/json" -d '[{"id":"en","name":"English"},{"id":"fr","name":"French"},{"id":"es","name":"Spanish"},{"id":"de","name":"German"},{"id":"ru","name":"Russian"},{"id":"no","name":"Norwegian"},{"id":"it","name":"Italian"},{"id":"pl","name":"Polish"},{"id":"pt","name":"Portuguese"},{"id":"sv","name":"Swedish"},{"id":"da","name":"Danish"},{"id":"ar","name":"Arabic"},{"id":"ch","name":"Chinese"},{"id":"ro","name":"Romanian"},{"id":"bg","name":"Bulgarian"},{"id":"cs","name":"Czech"},{"id":"sr","name":"Serbian"},{"id":"tr","name":"Turkish"},{"id":"gl","name":"Galician"},{"id":"nl","name":"Dutch"},{"id":"el","name":"Greek"},{"id":"fa","name":"Persian"},{"id":"th","name":"Thai"},{"id":"id","name":"Indonesian"},{"id":"hi","name":"Hindi"}]'

echo "\nset up autosuggest templates data"
curl -XPOST "http://${skipperHost}/admin/autosuggest/template" -H 'content-type: application/json'  -d '{"imageURL":"/images/template1.svg","templateName":"Double-box autosuggest with Popular products on right","templateId":"template-1","vertical":"fashion","jsConfig":{"popularProductsPresent":true}}'

curl -XPOST "http://${skipperHost}/admin/autosuggest/template" -H 'content-type: application/json'  -d '{"imageURL":"/images/template2.svg","templateName":"Double-box autosuggest with Popular products on left","templateId":"template-2","vertical":"fashion","jsConfig":{"popularProductsPresent":true}}'

curl -XPOST "http://${skipperHost}/admin/autosuggest/template" -H 'content-type: application/json'  -d '{"imageURL":"/images/template3.svg","templateName":"Single-box autosuggest with Popular products","templateId":"template-3","vertical":"fashion","jsConfig":{"popularProductsPresent":true}}'

curl -XPOST "http://${skipperHost}/admin/autosuggest/template" -H 'content-type: application/json'  -d '{"imageURL":"/images/template4.svg","templateName":"Single-box autosuggest without Popular products","templateId":"template-4","vertical":"fashion","jsConfig":{"popularProductsPresent":false}}'
