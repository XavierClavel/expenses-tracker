import axios from 'axios';


const backUrl = process.env.BACK_URL



const apiClient = axios.create({
    baseURL: backUrl,
    withCredentials: true, // to include cookies for session-based auth
    headers: {
        'Content-Type': 'application/json',
        'Cookie': `SESSION=${aaaaa}`,
    },
});


apiClient.interceptors.request.use(
    function (config) {
        return config
    },
    function (error) {
        return Promise.reject(error)
    }
)



apiClient.interceptors.response.use(
    function (response){
        return response
    },
    function (error) {
        console.log(error)
        console.log("intercepted error!")
        console.log(error.status)
        if (error.code == "ERR_NETWORK" || error.status == 502) {
            console.log("backend down")
        } else {
            return Promise.reject(error)
        }
    }
);

export default apiClient;