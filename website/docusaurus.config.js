const deployUrl = "https://weso.github.io"
const baseUrl = "/rdfshape-api/"
const docsUrl = `${deployUrl}${baseUrl}api/es/weso/rdfshape/`

/** @type {import('@docusaurus/types').DocusaurusConfig} */
module.exports = {
  title: "RDFShape API",
  tagline: "Processing and validation of RDF with ShEx, SHACL and more",
  organizationName: "weso", // GitHub org/user name.
  projectName: "rdfshape-api", // Repo name.
  url: deployUrl,
  baseUrl: baseUrl,
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "warn",
  favicon: "favicon.ico",
  trailingSlash: true,
  customFields: {
    docsUrl,
  },
  themeConfig: {
    image: "img/preview.png",
    hideableSidebar: false,
    colorMode: {
      defaultMode: "light",
      disableSwitch: false,
      respectPrefersColorScheme: true,
      switchConfig: {
        darkIcon: "🌙",
        lightIcon: "\u2600",
        darkIconStyle: {marginLeft: "2px"},
        lightIconStyle: {marginLeft: "1px"},
      },
    },
    navbar: {
      title: "RDFShape API",
      logo: {
        alt: "RDFShape API - WESO",
        src: "img/logo-weso.png",
      },
      items: [
        // Web docs
        {
          to: "/docs",
          label: "Web docs",
          position: "left",
        },
        // Scaladoc
        {
          to: docsUrl,
          label: "Scaladoc",
          position: "left",
        },
        // Link to repo
        {
          href: "https://github.com/weso/rdfshape-api",
          label: "GitHub",
          position: "right",
        },
      ],
    },
    footer: {
      style: "light",
      logo: {
        alt: "RDFShape API - WESO",
        src: "img/logo-weso-footer.png",
        href: "https://www.weso.es/",
      },
      links: [
        {
          title: "About us",
          items: [
            {
              label: "WESO Research Group",
              to: "https://www.weso.es/",
            },
            {
              label: "University of Oviedo",
              to: "https://www.uniovi.es/",
            },
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "GitHub",
              to: "https://github.com/weso",
            },
            {
              label: "Twitter",
              to: "https://twitter.com/wesoviedo",
            },
          ],
        },
        {
          title: "Further work",
          items: [
            {
              label: "RDFShape project",
              to: "https://github.com/weso/rdfshape",
            },
            {
              label: "More software by WESO",
              to: "https://www.weso.es/#software",
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} WESO Research Group`,
    },
  },
  presets: [
    [
      "@docusaurus/preset-classic",
      {
        docs: {
          path: "../rdfshape-docs/target/mdoc",
          sidebarPath: require.resolve("./sidebars.js"),
        },
        blog: false,
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
        sitemap: {},
      },
    ],
  ],
};
