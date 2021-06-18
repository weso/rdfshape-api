import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';
import Link from "@docusaurus/core/lib/client/exports/Link";

const FeatureList = [
    {
        title: 'Scaladoc',
        Svg: require('../../static/img/scala-icon.svg').default,
        description: (
            <>
                Check out the automatically generated Scaladoc, up to date with our latest stable
                build
            </>
        ),
        link: 'https://www.scala-lang.org/'
    },
    {
        title: 'Web documentation',
        Svg: require('../../static/img/webdocs.svg').default,
        description: (
            <>
                Friendly guides and short articles related to the project and the usage of the API
            </>
        ),
        link: '/docs'
    },
    {
        title: 'API Docs',
        Svg: require('../../static/img/rocket.svg').default,
        description: (
            <>
                Browse the API Docs and test the API directly in Swagger Hub without having to learn about the
                underlying infrastructure
            </>
        ),
        link: 'https://app.swaggerhub.com/apis/weso/RDFShape'
    },
];

function Feature({Svg, title, description, link}) {
    return (
        <div className={clsx('col col--4')}>
            <div className="text--center">
                <Svg className={styles.featureSvg} alt={title}/>
            </div>
            <div className="text--center padding-horiz--md">
                <Link to={link}><h3>{title}</h3></Link>
                <p>{description}</p>
            </div>
        </div>
    );
}

export default function HomepageFeatures() {
    return (
        <section className={styles.features}>
            <div className="container">
                <div className="row">
                    {FeatureList.map((props, idx) => (
                        <Feature key={idx} {...props} />
                    ))}
                </div>
            </div>
        </section>
    );
}
