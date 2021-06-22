import React from 'react'
import {Redirect} from '@docusaurus/router'
import useBaseUrl from '@docusaurus/useBaseUrl'

// Redirect to docs intro
const RdfShapeApi = () => {
    const redirectDocsIntro = useBaseUrl("/docs/home")
    return <Redirect to={redirectDocsIntro}/>
}

export default RdfShapeApi