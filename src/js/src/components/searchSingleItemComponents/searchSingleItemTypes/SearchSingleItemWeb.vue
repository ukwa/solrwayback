<template>
  <div class="singleEntryResult">
    <search-single-item-standard-info :rank="rankNumber" :result="result" />
    <search-single-item-images :hash="result.hash"
                               :url-norm="result.url_norm" 
                               :source="result.source_file_path" 
                               :offset="result.source_file_offset" 
                               input-type="multiple" />
    <search-single-item-all-data :id="result.id"
                                 :source="result.source_file_path"
                                 :offset="result.source_file_offset" />
  </div>
</template>

<script>
import SearchSingleItemStandardInfo from './../SearchSingleItemStandardInfo.vue'
import SearchSingleItemAllData from './../SearchSingleItemAllData.vue'
import SearchSingleItemImages from './../SearchSingleItemImages.vue'

//import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchSingleItemWeb',
  components: { 
    SearchSingleItemStandardInfo,
    SearchSingleItemAllData,
    SearchSingleItemImages
  },
  props: {
    result: {
      type: Object,
      required: true
    },
    rankNumber: {
      type:Number,
      required:true
    }
  },
  data () {
    return {     
    }
  },
  computed: {
  },
  mounted () {
  },
  
  methods: {
  }
}

</script>

    
