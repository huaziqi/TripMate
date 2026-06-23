<script>
import { useAuth } from '@/composables/useAuth'
import { initElder } from '@/composables/useElder'

export default {
	onLaunch: function() {
		const { loadFromStorage } = useAuth()
		loadFromStorage()
		initElder()
	},
	onShow: function() {},
	onHide: function() {}
}
</script>

<style>
	/*每个页面公共css */
</style>
