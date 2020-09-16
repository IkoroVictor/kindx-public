
const AWS = require('aws-sdk');
const createDOMPurify = require('dompurify');
const s3 = new AWS.S3()
const JSDOM = require('jsdom').JSDOM;
const Readability = require('./lib/Readability.js');
const Readerable = require('./lib/Readerable.js');
const options = {
    runScripts: "dangerously"
};
const BUCKET_NAME = process.env.READABILITY_BUCKET || "kindx-readable-html-files";
const METRIC_REF = "kindx.ops.readablilty.crawl";

exports.handler = async function (event) {
    return toReadable(event);
}



const toReadable = async (requestEvent) => {
    try {
        var dom = await JSDOM.fromURL(requestEvent.url, options);
        if (requestEvent.sanitize) {
            const window = new JSDOM('').window;
            const DOMPurify = createDOMPurify(window);
            dom = new JSDOM(DOMPurify.sanitize(dom.window.document.documentElement.innerHTML));
        }
        
        const document = dom.window.document;
        var readable = Readerable.isProbablyReaderable(document);
        var reader = new Readability(document);
        var article = reader.parse();

        var key = requestEvent.key || (Math.random().toString(36).substring(7) +  Math.random().toString(36).substring(7));
        await uploadFile(key, article.content).promise();
        logMetric(1, true);
        return {
            success: true,
            readable: readable,
            url: requestEvent.url,
            contentKey: key
        };
    }
    catch (error) {
        console.log(error);
        logMetric(1, false);
        return {
            success: false,
            message: error.message,
            url: requestEvent.url,
        };

    }
}

const uploadFile = (key, content) => {
    
    // Setting up S3 upload parameters
    const params = {
        Bucket: BUCKET_NAME,
        Key: key, 
        Body: content
    };

    // Uploading files to the bucket
    return s3.upload(params);
};


const logMetric = (value, success) => {
    console.log(JSON.stringify({
       m : METRIC_REF,
       e : Math.floor(Date.now() / 1000),
       v : value,
       t : [`success:${success}`]
    }));
}

//  toReadable({url : "https://mcdonalds.ee/menu/kuumad-joogid", sanitize : true}).then(e => {
//      console.log(e);
//  } )
