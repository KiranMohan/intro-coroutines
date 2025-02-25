package tasks

import contributors.*
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

fun loadContributorsCallbacks(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        log.debug("In getOrgReposCall().onResponse()")
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = Collections.synchronizedList(mutableListOf<User>())
        val indexCount = AtomicInteger(0)
        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                log.debug("In getRepoContributorsCall().onResponse()")
                logUsers(repo, responseUsers)
                val users = responseUsers.bodyList()
                allUsers += users
                if(indexCount.incrementAndGet() == repos.lastIndex) {
                    updateResults(allUsers.aggregate())
                }
            }
        }

    }
}

inline fun <T> Call<T>.onResponse(crossinline callback: (Response<T>) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            callback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            log.error("Call failed", t)
        }
    })
}
