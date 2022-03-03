import {customFields} from "./docusaurus.config"

const {scalaDocUrl, apiDocsUrl} = customFields

module.exports = {
  // Generate a sidebar from the docs folder structure
  // sidebar: [{type: "autogenerated", dirName: "."}],

  // Create a sidebar manually
  docsSidebar: [/* Home */
    {
      type: "doc", id: "home", label: "Welcome",
    },

    /* Category: deployment */
    {
      type: "category",
      label: "API Deployment",
      items: ["api-deployment/deployment_overview", "api-deployment/deployment_manual", "api-deployment/deployment_docker"],
      collapsed: false
    },

    /* Category: usage */
    {
      type: "category",
      label: "API Usage",
      items: ["api-usage/usage_cli"],
      collapsed: false
    },

    /* Category: testing */
    {
      type: "category",
      label: "API Testing and Auditing",
      items: ["api-testing-auditing/testing-auditing_munit", "api-testing-auditing/testing-auditing_integration", "api-testing-auditing/testing-auditing_logs"],
      collapsed: true
    },

    /* Category: documentation */
    {
      type: "category", label: "Additional documentation", items: [{
        type: "link", label: "Scaladoc", href: scalaDocUrl
      }, {
        type: "link", label: "API Docs (Swagger Hub)", href: apiDocsUrl
      }], collapsed: false
    },

    /* Webpage information */
    {
      type: "doc", id: "webpage/webpage_info", label: "About this webpage",
    },],

};
